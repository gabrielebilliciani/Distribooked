import random
import json
from pymongo import MongoClient
from bson import ObjectId
from faker import Faker
from datetime import datetime

# Initialize Faker
fake = Faker()

# MongoDB connection
client = MongoClient("mongodb://localhost:27017/")
db = client["library"]

# Load Users Dataset
with open("../datasets/users_dataset_05.json", "r") as f:
    users_data = json.load(f)

# Fetch Books Collection
books_collection = db["books"]
books = list(books_collection.find())

# Geolocation Ranges for Users
geo_ranges = {
    "Pisa": [(43.693033, 43.731535), (10.366360, 10.444071)],
    "Firenze": [(43.731467, 43.818066), (11.174652, 11.298083)],
    "Livorno": [(43.472290, 43.592005), (10.296033, 10.341677)],
    "Lucca": [(43.814299, 43.911694), (10.435017, 10.554528)],
}

# Helper to generate random geolocation in GeoJSON format
def generate_geolocation(city):
    lat_range, lon_range = geo_ranges[city]
    latitude = random.uniform(*lat_range)
    longitude = random.uniform(*lon_range)
    return {
        "type": "Point",
        "coordinates": [longitude, latitude]  # GeoJSON format: [longitude, latitude]
    }

# Generate User Embeddings
enhanced_users = []
for user in users_data:
    city = user["address"]["city"]
    user_location = generate_geolocation(city)

    # Generate readings
    readings = []
    num_readings = random.randint(0, 240)  # Max 80 books per year, 3 years (2022-2024)
    for _ in range(num_readings):
        book = random.choice(books)
        branch = random.choice(book["branches"])  # Select a random branch for book

        # Read branch location directly from MongoDB's GeoJSON format
        branch_location = branch["location"]

        readings.append({
            "id": str(book["_id"]),
            "title": book["title"],
            "authors": book["authors"],
            "returnDate": fake.date_between(start_date=datetime(2022, 1, 1), end_date=datetime(2024, 12, 31)).isoformat(),
            "branch": branch_location  # Store branch location in readings
        })

        # Update readingsCount in the books collection
        books_collection.update_one(
            {"_id": book["_id"]},
            {"$inc": {"readingsCount": 1}}
        )

    # Generate saved books
    saved_books = []
    num_saved_books = random.randint(0, 50)
    for _ in range(num_saved_books):
        book = random.choice(books)
        saved_books.append({
            "id": str(book["_id"]),
            "title": book["title"],
            "authors": book["authors"]
        })

    # Enhance user data
    enhanced_user = {
        **user,
        "location": user_location,  # Store user's location as GeoJSON
        "readings": readings,
        "savedBooks": saved_books
    }
    enhanced_users.append(enhanced_user)

# Insert Enhanced Users into MongoDB
users_collection = db["users"]
users_collection.insert_many(enhanced_users)

print("Users with geospatial data inserted successfully!")
