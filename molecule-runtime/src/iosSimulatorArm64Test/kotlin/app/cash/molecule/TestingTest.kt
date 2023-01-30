package app.cash.molecule

import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import platform.XCTest.XCTestCase
import platform.XCTest.XCUIApplication

class TestingTest : XCTestCase() {
  @Test fun sup() = runTest(dispatchTimeoutMs = 5_000) {
    val app = XCUIApplication()
    app.launch()

    println("ABOUT TO WAIT")
    DisplayLinkClock.withFrameNanos {
      // If this function does not time out the test passes.
    }

    app.terminate()
  }
}
