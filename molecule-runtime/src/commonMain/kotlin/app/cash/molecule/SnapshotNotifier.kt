/*
 * Copyright (C) 2025 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.molecule

/**
 * The different snapshot notification modes of Molecule.
 *
 * Compose uses snapshots to provide a consistent view of state. Mutations to state outside of the
 * composition are not automatically observed, and notifications of changes must be manually sent.
 * This can be achieved by registering a global write observer, for which you need at-minimum one
 * per process. Applications which use other Compose-based systems like Compose UI likely already
 * have one in place, whereas applications that only use Molecule need its automatic registering
 * of this notifier.
 *
 * @see androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications
 */
public enum class SnapshotNotifier {
  /**
   * Rely on some other external system for sending snapshot change notifications.
   *
   * This should only be used if you can guarantee that someone else is listening to global
   * snapshot writes and sending apply notifications. Usually this means that some other
   * Compose-based system is being used in your application, and that it will always be
   * initialized prior to Molecule or at the same time.
   *
   * Failure to ensure someone else is sending apply notifications will result in state writes
   * not triggering additional recomposition.
   *
   * Some examples where this policy can be used:
   * - On Android, using Compose UI _before_ Molecule (e.g., even just calling `setContent { }` on
   *   an `Activity` or `ComposeView`) will start a singleton snapshot write listener and applier.
   *   If you are sure that this will _always_ happen, specifying this policy for all Molecule
   *   launches is valid.
   * - On JetBrains' Compose UI for Desktop, calling the `application { }` function is enough to
   *   ensure their singleton snapshot write listener and applier is started (you do not have to
   *   even show a window).
   */
  External,

  /**
   * Register a global snapshot write observer and send apply notifications when new writes occur
   * using a coroutine launched on the same scope as the composition. This coroutine will be
   * canceled and observer unregistered when that scope is canceled.
   */
  WhileActive,
}
