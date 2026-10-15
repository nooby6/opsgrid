package dev.whrite.opsgrid

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OpsGridApplication

fun main(args: Array<String>) {
    runApplication<OpsGridApplication>(*args)
}
