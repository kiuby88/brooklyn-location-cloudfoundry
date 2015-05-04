/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package brooklyn.location.entity.cloudfoundry.java;


import brooklyn.enricher.basic.AbstractEnricher;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.*;
import brooklyn.entity.drivers.DriverDependentEntity;
import brooklyn.entity.drivers.EntityDriverManager;
import brooklyn.event.SensorEvent;
import brooklyn.event.SensorEventListener;
import brooklyn.event.feed.function.FunctionFeed;
import brooklyn.event.feed.function.FunctionPollConfig;
import brooklyn.location.Location;
import brooklyn.location.cloudfoundry.CloudFoundryPaasLocation;
import brooklyn.management.Task;
import brooklyn.policy.EnricherSpec;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.config.ConfigBag;
import brooklyn.util.exceptions.Exceptions;
import brooklyn.util.task.DynamicTasks;
import brooklyn.util.task.Tasks;
import brooklyn.util.time.CountdownTimer;
import brooklyn.util.time.Duration;
import brooklyn.util.time.Time;
import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import groovy.time.TimeDuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;


public class JavaCloudFoundryPaasWebAppImpl extends AbstractEntity
        implements JavaCloudFoundryPaasWebApp, DriverDependentEntity {


    private static final Logger log = LoggerFactory.getLogger(JavaCloudFoundryPaasWebAppImpl.class);

    private transient JavaPaasWebAppDriver driver;

    /**
     * @see #connectServiceUpIsRunning()
     */
    private volatile FunctionFeed serviceProcessIsRunning;
    protected boolean connectedSensors = false;

    public JavaCloudFoundryPaasWebAppImpl() {
        super(MutableMap.of(), null);
    }

    public JavaCloudFoundryPaasWebAppImpl(Entity parent) {
        this(MutableMap.of(), parent);
    }

    public JavaCloudFoundryPaasWebAppImpl(Map properties) {
        this(properties, null);
    }

    public JavaCloudFoundryPaasWebAppImpl(Map properties, Entity parent) {
        super(properties, parent);
    }

    @Override
    public String getApplicationName() {
        return getConfig(APPLICATION_NAME);
    }

    @Override
    public Class getDriverInterface() {

        return JavaPaasWebAppDriver.class;
    }

    protected void setProvisioningLocation(CloudFoundryPaasLocation location) {
        if (getAttribute(PAAS_LOCATION) != null) {
            throw new IllegalStateException("Cannot change paas location: existing=" +
                    getAttribute(PAAS_LOCATION) + "; new=" + location);
        }
        setAttribute(PAAS_LOCATION, location);
    }

    protected CloudFoundryPaasLocation getProvisioningLocation() {
        return getAttribute(PAAS_LOCATION);
    }

    @Override
    public JavaPaasWebAppDriver getDriver() {
        return driver;
    }


    protected CloudFoundryPaasLocation getLocationOrNull() {
        return Iterables.get(Iterables.filter(getLocations(), CloudFoundryPaasLocation.class), 0, null);
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    protected void initEnrichers() {
        super.initEnrichers();
        ServiceStateLogic.ServiceNotUpLogic
                .updateNotUpIndicator(this, SERVICE_PROCESS_IS_RUNNING,
                        "No information yet on whether this service is running");
        // add an indicator above so that if is_running comes through, the map is cleared and an update is guaranteed
        addEnricher(EnricherSpec.create(UpdatingNotUpFromServiceProcessIsRunning.class)
                .uniqueTag("service-process-is-running-updating-not-up"));
    }

    /**
     * This sub-class was copied directly from {@link SoftwareProcessImpl}
     * subscribes to SERVICE_PROCESS_IS_RUNNING and SERVICE_UP; the latter has no effect if
     * the former is set, but to support entities which set SERVICE_UP directly we want
     * to make sure that the absence of      * SERVICE_PROCESS_IS_RUNNING does not trigger any
     * not-up indicators
     */
    protected static class UpdatingNotUpFromServiceProcessIsRunning extends AbstractEnricher implements SensorEventListener<Object> {
        public UpdatingNotUpFromServiceProcessIsRunning() {
        }

        @Override
        public void setEntity(EntityLocal entity) {
            super.setEntity(entity);
            subscribe(entity, SERVICE_PROCESS_IS_RUNNING, this);
            subscribe(entity, Attributes.SERVICE_UP, this);
            onUpdated();
        }

        @Override
        public void onEvent(SensorEvent<Object> event) {
            onUpdated();
        }

        protected void onUpdated() {
            Boolean isRunning = entity.getAttribute(SERVICE_PROCESS_IS_RUNNING);
            if (Boolean.FALSE.equals(isRunning)) {
                ServiceStateLogic.ServiceNotUpLogic
                        .updateNotUpIndicator(entity, SERVICE_PROCESS_IS_RUNNING,
                                "The software process for this entity does not appear to be running");
                return;
            }
            if (Boolean.TRUE.equals(isRunning)) {
                ServiceStateLogic.ServiceNotUpLogic
                        .clearNotUpIndicator(entity, SERVICE_PROCESS_IS_RUNNING);
                return;
            }
            // no info on "isRunning"
            Boolean isUp = entity.getAttribute(Attributes.SERVICE_UP);
            if (Boolean.TRUE.equals(isUp)) {
                // if service explicitly set up, then don't apply our rule
                ServiceStateLogic.ServiceNotUpLogic
                        .clearNotUpIndicator(entity, SERVICE_PROCESS_IS_RUNNING);
                return;
            }
            // service not up, or no info
            ServiceStateLogic.ServiceNotUpLogic
                    .updateNotUpIndicator(entity, SERVICE_PROCESS_IS_RUNNING,
                            "No information on whether this service is running");
        }
    }

    /**
     * If custom behaviour is required by sub-classes, consider overriding {@link #doStart(Collection)})}.
     */
    @Override
    public final void start(final Collection<? extends Location> locations) {
        if (DynamicTasks.getTaskQueuingContext() != null) {
            doStart(locations);
        } else {
            Task<?> task = Tasks.builder().name("start (sequential)").body(new Runnable() {
                public void run() {
                    doStart(locations);
                }
            }).build();
            Entities.submit(this, task).getUnchecked();
        }
    }

    /**
     * It is a temporal method. It will be contains the start effector body. It will be
     * moved to LifeCycle class based on Task. It is the first approach.
     * It does not start the entity children.
     */
    protected final void doStart(Collection<? extends Location> locations) {

        ServiceStateLogic.setExpectedState(this, Lifecycle.STARTING);
        try {
            preStart();
            driver.start();
            log.info("Entity {} was started with driver {}", new Object[]{this, driver});

            postDriverStart();
            connectSensors();
            postStart();
            ServiceStateLogic.setExpectedState(this, Lifecycle.RUNNING);
        } catch (Throwable t) {
            ServiceStateLogic.setExpectedState(this, Lifecycle.ON_FIRE);
            throw Exceptions.propagate(t);
        }
    }

    /**
     * Called before driver.start; guarantees the driver will exist, and locations will
     * have been set.
     */
    protected void preStart() {
        createDriver();
    }

    /**
     * Create the driver ensuring that the location is ready.
     */
    private void createDriver() {
        CloudFoundryPaasLocation location = getLocationOrNull();
        if (location != null) {
            this.initDriver(location);
        } else {
            throw new ExceptionInInitializerError("Location should not be null in " + this +
                    " the driver needs a initialized Location");
        }
    }

    protected void initDriver(CloudFoundryPaasLocation location) {
        JavaPaasWebAppDriver newDriver = doInitDriver(location);
        if (newDriver == null) {
            throw new UnsupportedOperationException("cannot start " + this + " on " + location +
                    ": no driver available");
        }
        driver = newDriver;
    }

    /**
     * Creates the driver (if does not already exist or needs replaced for some reason).
     * Returns either the existing driver
     * or a new driver. Must not return null.
     */
    protected JavaPaasWebAppDriver doInitDriver(CloudFoundryPaasLocation location) {
        if (driver != null) {
            if (location.equals(driver.getLocation())) {
                return driver; //just reuse
            } else {
                log.warn("driver/location change is untested for {} at {}; " +
                        "changing driver and continuing", this, location);
                return newDriver(location);
            }
        } else {
            return newDriver(location);
        }
    }

    protected JavaPaasWebAppDriver newDriver(CloudFoundryPaasLocation loc) {
        EntityDriverManager entityDriverManager = getManagementContext().getEntityDriverManager();
        return (JavaPaasWebAppDriver) entityDriverManager.build(this, loc);

    }


    /**
     * Called after driver.start(). Default implementation is to wait to confirm the driver
     * definitely started application deployment.
     */
    protected void postDriverStart() {
        waitForEntityStart();
    }

    /**
     * Called after the rest of start has completed (after {@link #connectSensors()}
     * and {@link #waitForServiceUp()})
     */
    protected void postStart() {
        waitForServiceUp();
    }

    public void waitForServiceUp() {
        Duration timeout = getConfig(BrooklynConfigKeys.START_TIMEOUT);
        waitForServiceUp(timeout);
    }

    public void waitForServiceUp(Duration duration) {
        Entities.waitForServiceUp(this, duration);
    }

    public void waitForServiceUp(TimeDuration duration) {
        waitForServiceUp(duration.toMilliseconds(), TimeUnit.MILLISECONDS);
    }

    public void waitForServiceUp(long duration, TimeUnit units) {
        Entities.waitForServiceUp(this, Duration.of(duration, units));
    }

    /**
     * Copied direcly from {@link SoftwareProcessImpl}
     */
    // TODO Find a better way to detect early death of process.
    public void waitForEntityStart() {
        if (log.isDebugEnabled()) {
            log.debug("waiting to ensure {} doesn't abort prematurely", this);
        }
        Duration startTimeout = getConfig(START_TIMEOUT);
        CountdownTimer timer = startTimeout.countdownTimer();
        boolean isRunningResult = false;
        long delay = 100;
        while (!isRunningResult && !timer.isExpired()) {
            Time.sleep(delay);
            try {
                isRunningResult = driver.isRunning();
            } catch (Exception e) {
                ServiceStateLogic.setExpectedState(this, Lifecycle.ON_FIRE);
                // provide extra context info, as we're seeing this happen in strange circumstances
                if (driver == null) {
                    throw new IllegalStateException(this + " concurrent start and shutdown detected");
                }
                throw new IllegalStateException("Error detecting whether " + this + " is running: " + e, e);
            }
            if (log.isDebugEnabled()) {
                log.debug("checked {}, is running returned: {}", this, isRunningResult);
            }
            // slow exponential delay -- 1.1^N means after 40 tries and 50s elapsed, it reaches the max of 5s intervals
            // TODO use Repeater
            delay = Math.min(delay * 11 / 10, 5000);
        }
        if (!isRunningResult) {
            String msg = "Software process entity " + this + " did not pass is-running check within " +
                    "the required " + startTimeout + " limit (" + timer.getDurationElapsed().toStringRounded() + " elapsed)";
            log.warn(msg + " (throwing)");
            ServiceStateLogic.setExpectedState(this, Lifecycle.RUNNING);
            throw new IllegalStateException(msg);
        }
    }

    /**
     * For binding to the running app (e.g. connecting sensors to registry). Will be called
     * on start() and on rebind().
     * <p/>
     * Implementations should be idempotent (ie tell whether sensors already connected),
     * though the framework is pretty good about not calling when already connected.
     * TODO improve the framework's feed system to detect duplicate additions
     */
    protected void connectSensors() {
        connectedSensors = true;
        connectServiceUpIsRunning();
    }

    /**
     * For connecting the {@link #SERVICE_UP} sensor to the value of the
     * {@code getDriver().isRunning()} expression.
     * <p/>
     * Should be called inside {@link #connectSensors()}.
     *
     * @see #disconnectServiceUpIsRunning()
     */
    protected void connectServiceUpIsRunning() {
        serviceProcessIsRunning = FunctionFeed.builder()
                .entity(this)
                .period(Duration.FIVE_SECONDS)
                .poll(new FunctionPollConfig<Boolean, Boolean>(SERVICE_PROCESS_IS_RUNNING)
                        .onException(Functions.constant(Boolean.FALSE))
                        .callable(new Callable<Boolean>() {
                            public Boolean call() {
                                return getDriver().isRunning();
                            }
                        }))
                .build();
    }

    /**
     * For disconnecting from the running app. Will be called on stop.
     */
    protected void disconnectSensors() {
        connectedSensors = false;
        disconnectServiceUpIsRunning();
    }

    /**
     * For disconnecting the {@link #SERVICE_UP} feed.
     * <p/>
     * Should be called from {@link #disconnectSensors()}.
     *
     * @see #connectServiceUpIsRunning()
     */
    protected void disconnectServiceUpIsRunning() {
        if (serviceProcessIsRunning != null) serviceProcessIsRunning.stop();
        // set null so the SERVICE_UP enricher runs (possibly removing it), then remove so everything is removed
        // TODO race because the is-running check may be mid-task
        setAttribute(SERVICE_PROCESS_IS_RUNNING, null);
        removeAttribute(SERVICE_PROCESS_IS_RUNNING);
    }

    /**
     * If custom behaviour is required by sub-classes, consider overriding {@link #doStop()}.
     */
    @Override
    public final void stop() {
        if (DynamicTasks.getTaskQueuingContext() != null) {
            doStop();
        } else {
            Task<?> task = Tasks.builder().name("stop").body(new Runnable() {
                public void run() {
                    doStop();
                }
            }).build();
            Entities.submit(this, task).getUnchecked();
        }
    }

    /**
     * To be overridden instead of {@link #stop()}; sub-classes should call {@code super.doStop()} and should
     * add do additional work via tasks, executed using {@link DynamicTasks#queue(String, Callable)}.
     */
    protected final void doStop() {


        log.info("Stopping {} in {}", new Object[]{this, getLocationOrNull()});

        if (getAttribute(SERVICE_STATE_ACTUAL)
                .equals(Lifecycle.STOPPED)) {
            log.warn("The entity {} is already stopped", new Object[]{this});
            return;
        }

        ServiceStateLogic.setExpectedState(this, Lifecycle.STOPPING);
        try {
            preStop();
            driver.stop();
            postDriverStop();
            postStop();
            ServiceStateLogic.setExpectedState(this, Lifecycle.STOPPED);
            log.info("The entity stop operation {} is completed without errors",
                    new Object[]{this});
        } catch (Throwable t) {
            ServiceStateLogic.setExpectedState(this, Lifecycle.ON_FIRE);
            throw Exceptions.propagate(t);
        }

    }

    protected void preStop() {
        setAttribute(SERVICE_UP, false);
        disconnectSensors();

    }

    protected void postDriverStop() {
    }

    protected void postStop() {
    }


    /**
     * Called after this entity is fully rebound (i.e. it is fully managed).
     */
    protected void postRebind() {
    }

    protected void callRebindHooks() {
        Duration configuredMaxDelay = getConfig(MAXIMUM_REBIND_SENSOR_CONNECT_DELAY);
        if (configuredMaxDelay == null || Duration.ZERO.equals(configuredMaxDelay)) {
            connectSensors();
        } else {
            long delay = (long) (Math.random() * configuredMaxDelay.toMilliseconds());
            log.debug("Scheduled reconnection of sensors on {} in {}ms", this, delay);
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (getManagementSupport().isNoLongerManaged()) {
                            log.debug("Entity {} no longer managed; ignoring scheduled connect " +
                                    "sensors on rebind", JavaCloudFoundryPaasWebAppImpl.this);
                            return;
                        }
                        connectSensors();
                    } catch (Throwable e) {
                        log.warn("Problem connecting sensors on rebind of " +
                                JavaCloudFoundryPaasWebAppImpl.this, e);
                        Exceptions.propagateIfFatal(e);
                    }
                }
            }, delay);
        }
    }

    @Override
    public void onManagementStarting() {
        super.onManagementStarting();

        Lifecycle state = getAttribute(SERVICE_STATE_ACTUAL);
        if (state == null || state == Lifecycle.CREATED) {
            // Expect this is a normal start() sequence (i.e. start() will subsequently be called)
            setAttribute(SERVICE_UP, false);
            ServiceStateLogic.setExpectedState(this, Lifecycle.CREATED);
            // force actual to be created because this is expected subsequently
            setAttribute(SERVICE_STATE_ACTUAL, Lifecycle.CREATED);
        }
    }

    @Override
    public void onManagementStarted() {
        super.onManagementStarted();

        Lifecycle state = getAttribute(SERVICE_STATE_ACTUAL);
        if (state != null && state != Lifecycle.CREATED) {
            postRebind();
        }
    }

    @Override
    public void rebind() {
        Lifecycle state = getAttribute(SERVICE_STATE_ACTUAL);
        if (state == null || state != Lifecycle.RUNNING) {
            log.warn("On rebind of {}, not calling software process rebind hooks because " +
                    "state is {}", this, state);
            return;
        }

        // e.g. rebinding to a running instance
        // FIXME For rebind, what to do about things in STARTING or STOPPING state?
        // FIXME What if location not set?
        log.info("Rebind {} connecting to pre-running service", this);

        CloudFoundryPaasLocation location = getLocationOrNull();
        if (location != null) {
            initDriver(location);
            driver.rebind();
            if (log.isDebugEnabled())
                log.debug("On rebind of {}, re-created driver {}", this, driver);
        } else {
            log.info("On rebind of {}, no MachineLocation found (with locations {}) so not generating driver",
                    this, getLocations());
        }

        callRebindHooks();
    }

    /**
     * @deprecated since 0.7.0, this isn't a general test for modifiability, and was hardly ever
     * used (now never used)
     */
    public void checkModifiable() {
        Lifecycle state = getAttribute(SERVICE_STATE_ACTUAL);
        if (getAttribute(SERVICE_STATE_ACTUAL) == Lifecycle.RUNNING) return;
        if (getAttribute(SERVICE_STATE_ACTUAL) == Lifecycle.STARTING) return;
        throw new IllegalStateException("Cannot configure entity " + this + " in state " + state);
    }


    /**
     * If custom behaviour is required by sub-classes, consider overriding {@link #doRestart()}.
     */
    @Override
    public final void restart() {
        if (DynamicTasks.getTaskQueuingContext() != null) {
            doRestart(ConfigBag.EMPTY);
        } else {
            Task<?> task = Tasks.builder().name("restart").body(new Runnable() {
                public void run() {
                    doRestart(ConfigBag.EMPTY);
                }
            }).build();
            Entities.submit(this, task).getUnchecked();
        }
    }

    @Override
    public void kill() {

    }


    /**
     * To be overridden instead of {@link #restart()}; sub-classes should call {@code super.doRestart(ConfigBag)} and should
     * add do additional work via tasks, executed using {@link DynamicTasks#queue(String, Callable)}.
     */
    protected final void doRestart(ConfigBag parameters) {
    }

    protected final void doRestart() {
        doRestart(ConfigBag.EMPTY);
    }

    @Override
    public void destroy() {
        super.destroy();
        driver.deleteApplication();
    }


}
