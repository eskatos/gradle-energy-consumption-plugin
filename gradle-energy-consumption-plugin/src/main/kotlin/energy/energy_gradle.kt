package energy

import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Input
import org.gradle.internal.operations.*


abstract class EnergyMonitorService : BuildService<BuildServiceParameters.None>,
    BuildOperationListener,
    AutoCloseable {

    companion object {
        internal
        val logger = Logging.getLogger(EnergyMonitorService::class.java)
    }

    private val energyMonitor = EnergyMonitor()
    private val energyMonitorThread = Thread(energyMonitor, "Energy Monitor")

    init {
        energyMonitorThread.start()
        logger.info("Energy Monitor STARTED")
    }

    val processConsumedJoules: Double
        get() = energyMonitor.processConsumedJoules

    val systemConsumedJoules: Double
        get() = energyMonitor.systemConsumedJoules

    override fun close() {
        logger.info("Energy Monitor STOPPED")
        energyMonitorThread.interrupt()
    }

    override fun started(buildOperation: BuildOperationDescriptor, startEvent: OperationStartEvent) = Unit
    override fun progress(operationIdentifier: OperationIdentifier, progressEvent: OperationProgressEvent) = Unit
    override fun finished(buildOperation: BuildOperationDescriptor, finishEvent: OperationFinishEvent) = Unit
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
        EnergyMonitorService.logger.lifecycle(buildString {
            appendLine()
            appendLine("Energy consumption for this build invocation")
            append("  Gradle Daemon ")
            appendLine(String.format("%.4f Wh", monitorService.processConsumedJoules / 3600).padStart(16))
            append("  System ")
            appendLine(String.format("%.4f Wh", monitorService.systemConsumedJoules / 3600).padStart(16 + 7))
        })
    }
}
