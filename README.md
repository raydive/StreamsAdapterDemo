# DynamoDB Stream with KCL demo from AWS

https://docs.aws.amazon.com/ja_jp/amazondynamodb/latest/developerguide/Streams.KCLAdapter.Walkthrough.html から、読みやすいように一部変更しています。

## メモ

- KCLでDynamoDB streamを取り扱うときには、KCLv1とStreamAdapterが必要
- KCLに対してDynamoDB clientとStreamAdapterのClient、あとcloudwatchのclientがいる
- KCLで起動するworkerの実装と、そのFactoryの実装が必要
- FactoryをStreamsWorkerFactoryに渡すことによって、KCLがいい感じにFactoryからworkerを生成して取り扱ってくれる
- KinesisClientLibConfigurationのコンストラクタ引数がめちゃくちゃ多いので、ここは動きを知らないと取り扱い難しそう
  - Wrapperや他のライブラリでこの辺よきにはからってくれるものがないものか
- WorkerとFactoryはそれぞれIRecordProcessor、IRecordProcessorFactoryをimplする
- streamRecord.getDynamodb().getNewImage()で変更後の情報を取得できる
  - java.util.Map interfaceなので、そこから必要な情報に変換できそう
  - 同じkeyを変更するなら、IRecordProcessor内で畳み込んで処理ができるか
    - それよりか、IRecordProcessorではデータを取得するのみにして、後続の基盤にぶん投げるのが良さそうか
    - DynamoDB stream -> KCL -> なんらかのprocessorという形で変換していく
      - MapReduceだね

## KinesisClientLibConfigurationコンストラクタの引数説明

| パラメータ名                                          | 説明                                                                         |
|----------------------------------------------------|------------------------------------------------------------------------------|
| applicationName（アプリケーション名）               | Kinesisアプリケーションの名前。デフォルトでは、AWSリクエストのユーザーエージェント文字列に含まれる。トラブルシューティングに役立つ。         |
| streamName（ストリーム名）                          | Kinesisストリームの名前。                                                   |
| kinesisEndpoint（Kinesisエンドポイント）           | Kinesisサービスのエンドポイントを指定する。                                   |
| dynamoDBEndpoint（DynamoDBエンドポイント）         | DynamoDBサービスのエンドポイントを指定する。                                 |
| initialPositionInStream（初期位置）                | KinesisClientLibraryがアプリケーションを初めて起動した際のストリーム内のレコード取得の開始位置。LATESTまたはTRIM_HORIZONを選択。 |
| kinesisCredentialsProvider（Kinesisの認証情報プロバイダ） | Kinesisへのアクセスに使用する認証情報を提供する。                       |
| dynamoDBCredentialsProvider（DynamoDBの認証情報プロバイダ） | DynamoDBへのアクセスに使用する認証情報を提供する。                     |
| cloudWatchCredentialsProvider（CloudWatchの認証情報プロバイダ） | CloudWatchへのアクセスに使用する認証情報を提供する。                 |
| failoverTimeMillis（リース期間）                     | リースが更新されない場合、他のプロセスがリースを取得するまでの期間（ミリ秒）。     |
| workerId（ワーカーID）                              | Kinesisアプリケーション内の異なるワーカーやプロセスを区別するために使用。        |
| maxRecords（最大レコード数）                        | 1回のKinesis getRecords()呼び出しで取得する最大レコード数。                      |
| idleTimeBetweenReadsInMillis（Kinesisデータ取得間のアイドル時間） | Kinesisからデータを取得する際のアイドル時間（ミリ秒）。       |
| callProcessRecordsEvenForEmptyRecordList（空のレコードリストの場合の処理） | GetRecordsが空のレコードリストを返しても、IRecordProcessor::processRecords() APIを呼び出すかどうか。 |
| parentShardPollIntervalMillis（親シャードのポーリング間隔） | 親シャードが完了したかどうかを確認するためのポーリング間隔（ミリ秒）。 |
| shardSyncIntervalMillis（シャード同期間隔）           | リースとKinesisシャードの同期を行うタスク間の時間（ミリ秒）。                     |
| cleanupTerminatedShardsBeforeExpiry（終了したシャードのクリーンアップ） | 処理が完了したシャードをクリーンアップするかどうか。                               |
| kinesisClientConfig（Kinesisクライアントの設定）    | Kinesisクライアントに使用する設定オブジェクト。                                |
| dynamoDBClientConfig（DynamoDBクライアントの設定） | DynamoDBクライアントに使用する設定オブジェクト。                              |
| cloudWatchClientConfig（CloudWatchクライアントの設定） | CloudWatchクライアントに使用する設定オブジェクト。                          |
| taskBackoffTimeMillis（タスクのバックオフ時間）     | タスクが例外に遭遇した場合のバックオフ期間（ミリ秒）。                          |
| metricsBufferTimeMillis（メトリクスのバッファ時間）  | CloudWatchにパブリッシュされる前にメトリクスをバッファする最大時間（ミリ秒）。   |
| metricsMaxQueueSize（メトリクスの最大キューサイズ）  | CloudWatchにパブリッシュされる前にバッファリングする最大メトリクス数。         |
| validateSequenceNumberBeforeCheckpointing（チェックポイント前のシーケンス番号の検証） | KCLがチェックポイント前にAmazon Kinesisによって提供されたシーケンス番号を検証するかどうか。 |
| regionName（リージョン名）                         | サービスのリージョン名。                                                      |
| shutdownGraceMillis（Gracefulシャットダウン時間） | 強制的な終了前のGracefulシャットダウン時間（ミリ秒）。                           |
| billingMode（DynamoDBテーブル作成の請求モード）      | リーステーブル作成時に設定するDynamoDBの請求モード。                             |
| recordsFetcherFactory（レコードフェッチャーファクトリ） | 特定のシャードからデータを取得するためのレコードフェッチャーを生成するためのファクトリ。 |
| leaseCleanupIntervalMillis（リースクリーンアップ間隔） | リースのクリーンアップスレッドを実行する間隔（ミリ秒）。                         |
| completedLeaseCleanupThresholdMillis（クリーンアップしないリースの閾値） | クリーンアップする必要のある完了したリースの閾値（ミリ秒）。                     |
| garbageLeaseCleanupThresholdMillis（ガベージリースのクリーンアップ閾値） | クリーンアップする必要のあるガベージリースの閾値（ミリ秒）。                 |

