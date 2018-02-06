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

import com.aitusoftware.transport.factory.Media;
import com.aitusoftware.transport.factory.Service;
import com.aitusoftware.transport.factory.ServiceFactory;
import com.aitusoftware.transport.factory.SubscriberDefinition;
import com.aitusoftware.transport.factory.SubscriberThreading;
import com.aitusoftware.transport.net.ServerSocketFactoryImpl;
import com.aitusoftware.transport.threads.Idlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class PingServiceMain
{
    public static void main(String[] args) throws IOException, InterruptedException
    {
        Files.createDirectories(Paths.get(Config.PAGE_CACHE_ROOT));
        final Path pingServicePageCache = Paths.get(Config.PAGE_CACHE_ROOT, "ping");
        final ServiceFactory pingServiceFactory = new ServiceFactory(
                pingServicePageCache, new ServerSocketFactoryImpl(),
                new StaticAddressSpace(), c -> Idlers.busy(), SubscriberThreading.SINGLE_THREADED);
        final PingReceiver pingReceiver = pingServiceFactory.createPublisher(PingReceiver.class);
        final long pauseBetweenMessages = hertzToNanoInterval(Config.THROUGHPUT_MESSAGES_PER_SECOND);
        final PingSender pingSender = new PingSender(pingReceiver, pauseBetweenMessages,
                Config.PING_SENDER_AFFINITY);

        final Path pongServicePageCache = Paths.get(Config.PAGE_CACHE_ROOT, "pong");
        final ServiceFactory pongServiceFactory = new ServiceFactory(
                pongServicePageCache, new ServerSocketFactoryImpl(),
                new StaticAddressSpace(), c -> Idlers.busy(), SubscriberThreading.SINGLE_THREADED);
        final PongReceiver pongReceiverPublisher = pongServiceFactory.createPublisher(PongReceiver.class);
        pongServiceFactory.registerLocalSubscriber(
                new SubscriberDefinition<>(PingReceiver.class, new PingReceiverImpl(pongReceiverPublisher), Media.IPC),
                pingServicePageCache.resolve(ServiceFactory.PUBLISHER_PAGE_CACHE_PATH));

        final PongReceiverImpl pongReceiver = new PongReceiverImpl(pauseBetweenMessages);
        pingServiceFactory.registerLocalSubscriber(new SubscriberDefinition<>(PongReceiver.class,
                        pongReceiver, Media.IPC),
                pongServicePageCache.resolve(ServiceFactory.PUBLISHER_PAGE_CACHE_PATH));

        final Service pongService = pongServiceFactory.create();
        pongService.start();
        final Service pingService = pingServiceFactory.create();
        pingService.start();

        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final Future<?> sender = executorService.submit(pingSender::sendRequests);
        pongReceiver.getCompleteLatch().await();
        sender.cancel(true);
        executorService.shutdown();

        pongService.stop(5, TimeUnit.SECONDS);
        pingService.stop(5, TimeUnit.SECONDS);
    }

    private static long hertzToNanoInterval(final int messagesPerSecond)
    {
        return TimeUnit.SECONDS.toNanos(1) / messagesPerSecond;
    }
}
