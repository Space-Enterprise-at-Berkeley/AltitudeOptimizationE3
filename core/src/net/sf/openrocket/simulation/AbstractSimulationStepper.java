package net.sf.openrocket.simulation;

import java.util.Collection;

import net.sf.openrocket.masscalc.MassCalculator;
import net.sf.openrocket.masscalc.RigidBody;
import net.sf.openrocket.models.atmosphere.AtmosphericConditions;
import net.sf.openrocket.simulation.exception.SimulationException;
import net.sf.openrocket.simulation.listeners.SimulationListenerHelper;
import net.sf.openrocket.util.BugException;
import net.sf.openrocket.util.Coordinate;
import net.sf.openrocket.util.Quaternion;

public abstract class AbstractSimulationStepper implements SimulationStepper {
	
	protected static final double MIN_TIME_STEP = 0.001;
	
	/**
	 * Compute the atmospheric conditions, allowing listeners to override.
	 * 
	 * @param status	the simulation status
	 * @return			the atmospheric conditions to use
	 * @throws SimulationException	if a listener throws SimulationException
	 */
	protected AtmosphericConditions modelAtmosphericConditions(SimulationStatus status) throws SimulationException {
		AtmosphericConditions conditions;
		
		// Call pre-listener
		conditions = SimulationListenerHelper.firePreAtmosphericModel(status);
		if (conditions != null) {
			return conditions;
		}
		
		// Compute conditions
		double altitude = status.getRocketPosition().z + status.getSimulationConditions().getLaunchSite().getAltitude();
		conditions = status.getSimulationConditions().getAtmosphericModel().getConditions(altitude);
		
		// Call post-listener
		conditions = SimulationListenerHelper.firePostAtmosphericModel(status, conditions);
		
		checkNaN(conditions.getPressure());
		checkNaN(conditions.getTemperature());
		
		return conditions;
	}
	
	
	
	/**
	 * Compute the wind to use, allowing listeners to override.
	 * 
	 * @param status	the simulation status
	 * @return			the wind conditions to use
	 * @throws SimulationException	if a listener throws SimulationException
	 */
	protected Coordinate modelWindVelocity(SimulationStatus status) throws SimulationException {
		Coordinate wind;
		
		// Call pre-listener
		wind = SimulationListenerHelper.firePreWindModel(status);
		if (wind != null) {
			return wind;
		}
		
		// Compute conditions
		double altitude = status.getRocketPosition().z + status.getSimulationConditions().getLaunchSite().getAltitude();
		wind = status.getSimulationConditions().getWindModel().getWindVelocity(status.getSimulationTime(), altitude);
		
		// Call post-listener
		wind = SimulationListenerHelper.firePostWindModel(status, wind);
		
		checkNaN(wind);
		
		return wind;
	}
	
	
	
	/**
	 * Compute the gravity to use, allowing listeners to override.
	 * 
	 * @param status	the simulation status
	 * @return			the gravitational acceleration to use
	 * @throws SimulationException	if a listener throws SimulationException
	 */
	protected double modelGravity(SimulationStatus status) throws SimulationException {
		double gravity;
		
		// Call pre-listener
		gravity = SimulationListenerHelper.firePreGravityModel(status);
		if (!Double.isNaN(gravity)) {
			return gravity;
		}
		
		// Compute conditions
		gravity = status.getSimulationConditions().getGravityModel().getGravity(status.getRocketWorldPosition());
		
		// Call post-listener
		gravity = SimulationListenerHelper.firePostGravityModel(status, gravity);
		
		checkNaN(gravity);
		
		return gravity;
	}
	
	
	
	/**
	 * Compute the mass data to use, allowing listeners to override.
	 * 
	 * @param status	the simulation status
	 * @return			the mass data to use
	 * @throws SimulationException	if a listener throws SimulationException
	 */
	protected RigidBody calculateStructureMass(SimulationStatus status) throws SimulationException {
		RigidBody structureMass;
		
		// Call pre-listener
		structureMass = SimulationListenerHelper.firePreMassCalculation(status);
		if (structureMass != null) {
			return structureMass;
		}
		
		structureMass = MassCalculator.calculateStructure( status.getConfiguration() );  
						
		// Call post-listener
		structureMass = SimulationListenerHelper.firePostMassCalculation(status, structureMass);
		
		checkNaN(structureMass.getCenterOfMass());
		checkNaN(structureMass.getLongitudinalInertia());
		checkNaN(structureMass.getRotationalInertia());
		
		return structureMass;
	}
	
	protected RigidBody calculateMotorMass(SimulationStatus status) throws SimulationException {
		RigidBody motorMass;
		
		// Call pre-listener
		motorMass = SimulationListenerHelper.firePreMassCalculation(status);
		if (motorMass != null) {
			return motorMass;
		}
		
		motorMass = MassCalculator.calculateMotor( status );  

				
		// Call post-listener
		motorMass = SimulationListenerHelper.firePostMassCalculation(status, motorMass);
		
		checkNaN(motorMass.getCenterOfMass());
		checkNaN(motorMass.getLongitudinalInertia());
		checkNaN(motorMass.getRotationalInertia());
		
		return motorMass;
	}
	
	
	/**
	 * Calculate the thrust produced by the motors in the current configuration, allowing
	 * listeners to override.  The thrust is calculated at time <code>status.time</code>.
	 * <p>
	 * TODO: HIGH:  This method does not take into account any moments generated by off-center motors.
	 *  
	 * @param status					the current simulation status.
	 * @param acceleration				the current (approximate) acceleration
	 * @param atmosphericConditions		the current atmospheric conditions
	 * @param stepMotors				whether to step the motors forward or work on a clone object
	 * @return							the thrust at the specified time
	 */
	protected double calculateThrust(SimulationStatus status,
			double acceleration, AtmosphericConditions atmosphericConditions,
			boolean stepMotors) throws SimulationException {
		double thrust;
		
		// Pre-listeners
		thrust = SimulationListenerHelper.firePreThrustCalculation(status);
		if (!Double.isNaN(thrust)) {
			return thrust;
		}
		
		thrust = 0;
		Collection<MotorClusterState> activeMotorList = status.getActiveMotors();
		for (MotorClusterState currentMotorState : activeMotorList ) {
			thrust += currentMotorState.getThrust( status.getSimulationTime() );

		}
		
		// Post-listeners
		thrust = SimulationListenerHelper.firePostThrustCalculation(status, thrust);
		
		checkNaN(thrust);
		
		return thrust;
	}
	
	
	/**
	 * Check that the provided value is not NaN.
	 * 
	 * @param d					the double value to check.
	 * @throws BugException		if the value is NaN.
	 */
	protected void checkNaN(double d) {
		if (Double.isNaN(d)) {
			throw new BugException("Simulation resulted in not-a-number (NaN) value, please report a bug.");
		}
	}
	
	/**
	 * Check that the provided coordinate is not NaN.
	 * 
	 * @param c					the coordinate value to check.
	 * @throws BugException		if the value is NaN.
	 */
	protected void checkNaN(Coordinate c) {
		if (c.isNaN()) {
			throw new BugException("Simulation resulted in not-a-number (NaN) value, please report a bug, c=" + c);
		}
	}
	
	
	/**
	 * Check that the provided quaternion is not NaN.
	 * 
	 * @param q					the quaternion value to check.
	 * @throws BugException		if the value is NaN.
	 */
	protected void checkNaN(Quaternion q) {
		if (q.isNaN()) {
			throw new BugException("Simulation resulted in not-a-number (NaN) value, please report a bug, q=" + q);
		}
	}
}
