/*
 * Copyright 2016 - 2017 Aitu Software Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aitusoftware.example.application.ipc.ping;

import com.aitusoftware.transport.messaging.Topic;

@Topic
public interface PingReceiver
{
    void send(final long requestNanos, final long sequenceNumber,
              final long padding0, final long padding1,
              final long padding2, final long padding3,
              final long padding4, final long padding5);
}