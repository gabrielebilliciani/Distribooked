from pymongo import MongoClient
import redis

# MongoDB connection details
MONGO_URI = "mongodb://localhost:27017"
MONGO_DB_NAME = "library"

# Redis connection details
REDIS_HOST = "127.0.0.1"
REDIS_PORT = 6379

# Connect to MongoDB
mongo_client = MongoClient(MONGO_URI)
db = mongo_client[MONGO_DB_NAME]
books_collection = db.books

# Connect to Redis
redis_client = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True)

# Fetch books from MongoDB
books = books_collection.find({}, {"_id": 1, "branches": 1})

# Insert data into Redis
record_count = 0
for book in books:
    book_id = str(book["_id"])  # Convert ObjectId to string
    branches = book.get("branches", [])
    
    if not branches:
        print(f"Skipping book {book_id}: No branches available")
        continue  # Skip books without branches
    
    for branch in branches:
        library_id = str(branch["_id"])  # Convert ObjectId to string
        number_of_copies = branch.get("numberOfCopies", 0)
        
        # Store in Redis using the required format
        redis_key = f"book:{book_id}:lib:{library_id}:avail"
        redis_client.set(redis_key, number_of_copies)
        record_count += 1
        

print(f"✅ Successfully inserted {record_count} records into Redis.")
