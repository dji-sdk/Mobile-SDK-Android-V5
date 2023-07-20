package dji.sampleV5.aircraft.control

class PidController(private val kp: Float, private val ki: Float, private val kd: Float) {
    private var lastError: Double = 0.0
    private var derivative: Double = 0.0
    private var integral: Double = 0.0
    private var setpoint: Double = 0.0
    val maxVelocity: Double = 2.0 // m/s
    val tolerance: Double = 0.1 // m

    fun getControl(currentval: Double): Double {
        var error = calculateError(currentval)
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