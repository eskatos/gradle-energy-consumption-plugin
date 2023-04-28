import java.security.MessageDigest

tasks.register("work") {
    doLast {
        println("Doing some work")
        repeat(3000) { _ ->
            Thread.sleep(2)
            MessageDigest.getInstance("SHA-256")
                .digest("T${System.currentTimeMillis()}".toByteArray())
                .fold("") { str, it -> str + "%02x".format(it) }
        }
        println("Done with the work")
    }
}
