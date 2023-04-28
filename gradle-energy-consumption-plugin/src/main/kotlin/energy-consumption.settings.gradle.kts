import energy.EnergyMonitorService
import energy.ReportEnergyConsumption
import org.gradle.kotlin.dsl.support.serviceOf

val flowScope = serviceOf<FlowScope>()
val flowProviders = serviceOf<FlowProviders>()

val service = gradle.sharedServices.registerIfAbsent("energyMonitorService", EnergyMonitorService::class) {}
service.get()
flowScope.always(ReportEnergyConsumption::class) {
    parameters.apply {
        energyMonitorService.set(service)
        buildFinished.set(flowProviders.buildWorkResult.map { })
    }
}
