package energy

import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Input


abstract class EnergyMonitorService : BuildService<BuildServiceParameters.None>, AutoCloseable {

    private val energyMonitor = EnergyMonitor()
    private val energyMonitorThread = Thread(energyMonitor)

    init {
        energyMonitorThread.start()
        println("Energy Monitor Service Started")
    }

    val processConsumedJoules: Double
        get() = energyMonitor.processConsumedJoules

    val systemConsumedJoules: Double
        get() = energyMonitor.systemConsumedJoules

    override fun close() {
        energyMonitorThread.interrupt()
    }
}


abstract class ReportEnergyConsumption : FlowAction<ReportEnergyConsumption.Params> {

    interface Params : FlowParameters {

        @get:Input
        val buildFinished: Property<Unit>

        @get:Input
        val energyMonitorService: Property<EnergyMonitorService>
    }

    override fun execute(parameters: Params) {
        val monitorService = parameters.energyMonitorService.get()
        println(buildString {
            appendLine()
            appendLine("-".repeat(76))
            appendLine("Energy consumption for this build invocation")
            append("  Gradle Daemon ")
            appendLine(String.format("%.4f Wh", monitorService.processConsumedJoules / 3600).padStart(16))
            append("  System ")
            appendLine(String.format("%.4f Wh", monitorService.systemConsumedJoules / 3600).padStart(16 + 7))
            appendLine("-".repeat(76))
        })
    }
}
