package social.vyb.app.features.stories

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TenantIdResolverTest {
    @Test
    fun concurrentResolutionForOneUserUsesOneLookup() = runBlocking {
        val resolver = TenantIdResolver()
        val lookupCount = AtomicInteger()

        val resolved = List(8) {
            async {
                resolver.resolve("user-a") {
                    lookupCount.incrementAndGet()
                    delay(20)
                    "tenant-a"
                }
            }
        }.awaitAll()

        assertEquals(List(8) { "tenant-a" }, resolved)
        assertEquals(1, lookupCount.get())
    }

    @Test
    fun accountSwitchNeverReusesAnotherUsersTenant() = runBlocking {
        val resolver = TenantIdResolver()
        val lookupCount = AtomicInteger()

        assertEquals(
            "tenant-a",
            resolver.resolve("user-a") {
                lookupCount.incrementAndGet()
                "tenant-a"
            }
        )
        assertEquals(
            "tenant-b",
            resolver.resolve("user-b") {
                lookupCount.incrementAndGet()
                "tenant-b"
            }
        )
        assertEquals(2, lookupCount.get())
    }

    @Test
    fun invalidationForCurrentUserForcesFreshLookup() = runBlocking {
        val resolver = TenantIdResolver()
        val lookupCount = AtomicInteger()

        resolver.resolve("user-a") {
            lookupCount.incrementAndGet()
            "tenant-a"
        }
        resolver.invalidate("user-a")
        resolver.resolve("user-a") {
            lookupCount.incrementAndGet()
            "tenant-a"
        }

        assertEquals(2, lookupCount.get())
    }
}
