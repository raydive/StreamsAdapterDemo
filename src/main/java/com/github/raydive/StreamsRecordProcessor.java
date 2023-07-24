/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * This file is licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License. A copy of
 * the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.github.raydive;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason;
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput;

import java.nio.charset.StandardCharsets;

public class StreamsRecordProcessor implements IRecordProcessor {
    private Integer checkpointCounter;

    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    public StreamsRecordProcessor(AmazonDynamoDB dynamoDBClient2, String tableName) {
        this.dynamoDBClient = dynamoDBClient2;
        this.tableName = tableName;
    }

    @Override
    public void initialize(InitializationInput initializationInput) {
        checkpointCounter = 0;
    }

    @Override
    public void processRecords(ProcessRecordsInput processRecordsInput) {
        processRecordsInput.getRecords().forEach(record -> {
            var data = new String(record.getData().array(), StandardCharsets.UTF_8);
            System.out.println(data);
            if (record instanceof RecordAdapter) {
                var streamRecord = ((RecordAdapter) record)
                        .getInternalObject();

                switch (streamRecord.getEventName()) {
                    case "INSERT", "MODIFY" -> StreamsAdapterDemoHelper.putItem(dynamoDBClient, tableName,
                            streamRecord.getDynamodb().getNewImage()); // NewImageだけ相手にしたいなら、getNewImage呼び出してよしなに処理すれば良さそう
                    case "REMOVE" -> StreamsAdapterDemoHelper.deleteItem(dynamoDBClient, tableName,
                            streamRecord.getDynamodb().getKeys().get("Id").getN());
                }
            }
            checkpointCounter += 1;
            if (checkpointCounter % 10 == 0) {
                try {
                    // ここで10件ずつcheckpointを入れる
                    // recordからsequence numberを取得できるから、sequence numberでこの辺を処理することになるかな
                    processRecordsInput.getCheckpointer().checkpoint();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    public void shutdown(ShutdownInput shutdownInput) {
        // ShutdownReasonは以下の３つが存在する。ZOMBIEはcheckpointを入れないこと(should)
        // ZOMBIE
        // TERMINATE
        // REQUESTED
        if (shutdownInput.getShutdownReason() == ShutdownReason.TERMINATE) {
            try {
                shutdownInput.getCheckpointer().checkpoint();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}

