# Gradle Energy Consumption Plugin

> **Warning**: this is just a proof-of-concept experiment!
> 
> Only Linux hosts are supported, only CPU and DRAM consumption is monitored, Gradle's configuration cache is not supported, monitoring starts too late, only the Gradle daemon process is monitored, data is not fed to build scans etc...
> 
> We could potentially use sampling in order to attribute energy consumption to units of work.

## Set up the environment

Java >= 14 is required, tested with Java 17.

[CVE-2020-8694](https://www.cve.org/CVERecord?id=CVE-2020-8694) caused all Linux distributions to change the permissions of the RAPL files. They can only be read by root.

Change the files permissions in order for this experiment to run:

```shell
chmod a+r /sys/class/powercap/intel-rapl/intel-rapl:1/energy_uj
```

Note that the above is not persistent and will be reset on reboot.
This can be persistently changed with:

```shell
sudo apt install sysfsutils
echo "mode class/powercap/intel-rapl:1/energy_uj = 0444" | sudo tee -a /etc/sysfs.conf > /dev/null
sudo reboot # CAREFUL!
```


## Try it out

By running the following command you will build the plugin locally and run a task that does some work:

```shell
./gradlew work
```

If you want to try it on your own build you can include the plugin build and apply the plugin in your settings:

`settings.gradle.kts`
```kotlin
pluginManagement {
    includeBuild("<path-to-this-repository>/gradle-energy-consumption-plugin")
}
plugins {
    id("energy-consumption")
}
```
