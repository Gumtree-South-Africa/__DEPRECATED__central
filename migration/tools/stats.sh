#! /bin/sh

# This script comes in handy when you want to run migration with deep comparison over a number of conversationIds
#
# Create a file with the following contents:
#
# localhost:8080/coremigration/conversation/101m:4pcv1b9:27zcx9h1w
# localhost:8080/coremigration/conversation/101v:4pcv1b9:27zcx9h1w
# ... etc ...
#
# Then run:
#
# time while read c; do echo $c >> migrate_per_conversation.log; time curl $c | tee -a migrate_per_conversation.log; echo >> migrate_per_conversation.log; done < file_with_urls
#
# Note that some conversations will time out, this takes two minutes per conversation. You might want to pass -m5 to curl, which will
# speed up the migration by a lot. Downside is that the timed out ids no longer show up in the migrate_per_conversation.log file.
#
# This will output a migrate_per_conversation.log. To keep an eye on this log file, run:
#
# watch -n1 $0 migrate_per_conversation.log

echo "Migrated conversations:      $(grep -i conversationid $1 | grep -ci saving)"
echo "Event in C* but not in Riak: $(grep -i conversationid $1 | grep -ci 'not in riak')"
echo "Timed-out conversations:     $(grep -i conversationid $1 | grep -ci timeout)"
echo "Conversation not in Riak:    $(grep -ic 'No conversationEvents found in Riak' migrate_per_conversation.log)"
echo "Equal conversations:         $(grep -i conversationid $1 | grep -ci equal)"
echo
echo "Migrated conversations:      $(grep ^localhost $1 | wc -l)"
echo "Total conversations:         $(wc -l c)"
