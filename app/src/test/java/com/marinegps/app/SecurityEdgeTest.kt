package com.marinegps.app
import org.junit.Test
import org.junit.Assert.*
class SecurityEdgeTest {
 @Test(expected=IllegalArgumentException::class) fun oversizedQr(){decodeQrPayload("x".repeat(8193))}
 @Test(expected=IllegalArgumentException::class) fun invalidGeo(){parseGeo("geo:91,45")}
 @Test fun malformedWreckRows(){assertTrue(parseWrecks("""{"features":[null,42,{},{"geometry":{"type":"Point","coordinates":[]}}]}""").isEmpty())}
}
