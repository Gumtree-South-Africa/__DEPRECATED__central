HOST="replyts-devenv-17493.phx-os1.stratus.dev.ebay.com:9200"
curl -XPUT "http://$HOST/replyts/message/_mapping" -d @message_mapping.json
