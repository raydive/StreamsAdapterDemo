/*
  Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.

  This file is licensed under the Apache License, Version 2.0 (the "License").
  You may not use this file except in compliance with the License. A copy of
  the License is located at

  http://aws.amazon.com/apache2.0/

  This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
  CONDITIONS OF ANY KIND, either express or implied. See the License for the
  specific language governing permissions and limitations under the License.
 */
package com.github.raydive;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StreamsAdapterDemoHelper {

    /**
     * @return StreamArn
     */
    public static String createTable(AmazonDynamoDB client, String tableName) {
        var attributeDefinitions = new ArrayList<AttributeDefinition>();
        attributeDefinitions.add(new AttributeDefinition().withAttributeName("Id").withAttributeType("N"));

        var keySchema = new ArrayList<KeySchemaElement>();
        keySchema.add(new KeySchemaElement().withAttributeName("Id").withKeyType(KeyType.HASH)); // Partition
        // key

        var provisionedThroughput = new ProvisionedThroughput()
                .withReadCapacityUnits(2L)
                .withWriteCapacityUnits(2L);

        var streamSpecification = new StreamSpecification()
                .withStreamEnabled(true)
                .withStreamViewType(StreamViewType.NEW_IMAGE);
        var createTableRequest = new CreateTableRequest()
                .withTableName(tableName)
                .withAttributeDefinitions(attributeDefinitions)
                .withKeySchema(keySchema)
                .withProvisionedThroughput(provisionedThroughput)
                .withStreamSpecification(streamSpecification);

        try {
            System.out.println("Creating table " + tableName);
            var result = client.createTable(createTableRequest);
            return result.getTableDescription().getLatestStreamArn();
        }
        catch (ResourceInUseException e) {
            System.out.println("Table already exists.");
            return describeTable(client, tableName).getTable().getLatestStreamArn();
        }
    }

    public static DescribeTableResult describeTable(AmazonDynamoDB client, String tableName) {
        return client.describeTable(new DescribeTableRequest().withTableName(tableName));
    }

    public static ScanResult scanTable(AmazonDynamoDB dynamoDBClient, String tableName) {
        return dynamoDBClient.scan(new ScanRequest().withTableName(tableName));
    }

    public static void putItem(AmazonDynamoDB dynamoDBClient, String tableName, String id, String val) {
        var item = new HashMap<String, AttributeValue>();
        item.put("Id", new AttributeValue().withN(id));
        item.put("attribute-1", new AttributeValue().withS(val));

        PutItemRequest putItemRequest = new PutItemRequest().withTableName(tableName).withItem(item);
        dynamoDBClient.putItem(putItemRequest);
    }

    public static void putItem(AmazonDynamoDB dynamoDBClient, String tableName,
                               Map<String, AttributeValue> items) {
        PutItemRequest putItemRequest = new PutItemRequest().withTableName(tableName).withItem(items);
        dynamoDBClient.putItem(putItemRequest);
    }

    public static void updateItem(AmazonDynamoDB dynamoDBClient, String tableName, String id, String val) {
        var key = new HashMap<String, AttributeValue>();
        key.put("Id", new AttributeValue().withN(id));

        var attributeUpdates = new HashMap<String, AttributeValueUpdate>();
        AttributeValueUpdate update = new AttributeValueUpdate().withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withS(val));
        attributeUpdates.put("attribute-2", update);

        UpdateItemRequest updateItemRequest = new UpdateItemRequest().withTableName(tableName).withKey(key)
                .withAttributeUpdates(attributeUpdates);
        dynamoDBClient.updateItem(updateItemRequest);
    }

    public static void deleteItem(AmazonDynamoDB dynamoDBClient, String tableName, String id) {
        var key = new HashMap<String, AttributeValue>();
        key.put("Id", new AttributeValue().withN(id));

        DeleteItemRequest deleteItemRequest = new DeleteItemRequest().withTableName(tableName).withKey(key);
        dynamoDBClient.deleteItem(deleteItemRequest);
    }

    public static AwsClientBuilder.EndpointConfiguration getDynamoDBEndpointConfiguration() {
        return new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-west-2");
    }

    public static AwsClientBuilder.EndpointConfiguration getDynamoDBStreamsEndpointConfiguration() {
        return new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-west-2");
    }

    public static AwsClientBuilder.EndpointConfiguration getCloudWatchEndpointConfiguration() {
        return new AwsClientBuilder.EndpointConfiguration("http://localhost:4566", "us-west-2");
    }
}

