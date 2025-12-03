package net.opendasharchive.openarchive.services.storacha.model

data class PlanInfo(
    val name: String,
    val storageLimit: Long, // in bytes
    val egressLimit: Long, // in bytes
    val monthlyCost: Double, // in USD
    val additionalStorageCost: Double, // per GB per month
    val description: String,
) {
    companion object {
        fun fromPlanProduct(planProduct: String?): PlanInfo =
            when {
                planProduct?.contains("starter", ignoreCase = true) == true -> STARTER
                planProduct?.contains("lite", ignoreCase = true) == true -> LITE
                planProduct?.contains("business", ignoreCase = true) == true -> BUSINESS
                else -> STARTER // Default to starter plan
            }

        val STARTER =
            PlanInfo(
                name = "Starter",
                storageLimit = 5L * 1024 * 1024 * 1024, // 5GB
                egressLimit = 5L * 1024 * 1024 * 1024, // 5GB
                monthlyCost = 0.0,
                additionalStorageCost = 0.15,
                description = "Free tier with 5GB storage and egress",
            )

        val LITE =
            PlanInfo(
                name = "Lite",
                storageLimit = 100L * 1024 * 1024 * 1024, // 100GB
                egressLimit = 100L * 1024 * 1024 * 1024, // 100GB
                monthlyCost = 10.0,
                additionalStorageCost = 0.05,
                description = "Cheapest per GB - 100GB storage and egress",
            )

        val BUSINESS =
            PlanInfo(
                name = "Business",
                storageLimit = 2L * 1024 * 1024 * 1024 * 1024, // 2TB
                egressLimit = 2L * 1024 * 1024 * 1024 * 1024, // 2TB
                monthlyCost = 100.0,
                additionalStorageCost = 0.03,
                description = "Enterprise tier with 2TB storage and egress",
            )
    }

    fun formatAllocation(): String {
        val storage = formatBytes(storageLimit)
        val additionalCostFormatted = String.format("$%.3f", additionalStorageCost)
        return if (monthlyCost == 0.0) {
            "$storage free, then $additionalCostFormatted/GB"
        } else {
            "$storage included, then $additionalCostFormatted/GB"
        }
    }

    fun formatMonthlyCost(): String =
        if (monthlyCost == 0.0) {
            "Free"
        } else {
            String.format("$%.0f/month", monthlyCost)
        }

    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return if (size == size.toLong().toDouble()) {
            "${size.toLong()}${units[unitIndex]}"
        } else {
            String.format("%.1f%s", size, units[unitIndex])
        }
    }
}
