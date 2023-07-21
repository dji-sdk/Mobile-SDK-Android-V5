package dji.sampleV5.aircraft.control

class PidController(private val kp: Float, private val ki: Float, private val kd: Float) {
    private var lastError: Double = 0.0
    private var derivative: Double = 0.0
    private var integral: Double = 0.0
    private var setpoint: Double = 0.0
    val maxVelocity: Double = 2.0 // m/s
    val posTolerance: Double = 0.005 // m ##TODO need to convert to meters
    val yawTolerance: Double = 1.0 // degrees
    val altTolerance: Double = 10.0 // feet

    fun getControl(currentval: Double): Double {
        val error = calculateError(currentval)
        integral += error
        derivative = error - lastError
        lastError = error
        return (kp * error) + (ki * integral) + (kd * derivative)
    }
    fun calculateError(currentval: Double): Double {
        return (this.setpoint - currentval)
    }

    fun setSetpoint(setpoint: Double) {
        this.setpoint = setpoint
    }
}