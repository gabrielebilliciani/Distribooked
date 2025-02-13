import json
import random
from pymongo import MongoClient
from bson import ObjectId

# MongoDB Connection
client = MongoClient("mongodb://localhost:27017/")
db = client["library"]  # Replace with your DB name

# Load Branches Dataset
with open("../datasets/branches_dataset.json", "r") as f:
    branches_data = json.load(f)

# Step 1: Insert Branches into MongoDB
branches_collection = db["branches"]

# Pre-generate Branch IDs and insert branches
branch_docs = []  # Store inserted branch documents with their IDs
for branch in branches_data:
    branch_doc = {
        "_id": ObjectId(),
        "isilCode": branch["codice_isil"],
        "name": branch["denominazione"],
        "address": {
            "street": branch["indirizzo"],
            "city": branch["comune"],
            "province": branch["provincia"],
            "postalCode": branch["cap"],
            "country": "Italia"
        },
        "location": {  # GeoJSON format for geospatial indexing
            "type": "Point",
            "coordinates": [branch["longitudine"], branch["latitudine"]]  # Longitude first!
        },
        "phone": branch["telefono"],
        "email": branch["email"],
        "url": branch["url"]
    }
    branches_collection.insert_one(branch_doc)
    branch_docs.append(branch_doc)  # Save the document with the generated _id

print("Branches inserted successfully!")

# Step 2: Assign Branches to Books
books_collection = db["books"]

# Retrieve all books
all_books = list(books_collection.find())

# Initialize round-robin index
branch_count = len(branch_docs)
round_robin_index = 0

# Process books in batches of 10
batch_size = 10
for i in range(0, len(all_books), batch_size):
    # Get the current batch of books
    batch_books = all_books[i:i + batch_size]

    # Assign to primary branch
    primary_branch = branch_docs[round_robin_index]
    round_robin_index = (round_robin_index + 1) % branch_count

    # Choose 3 additional random branches for the batch
    additional_branches = random.sample(
        [b for b in branch_docs if b["_id"] != primary_branch["_id"]], 3
    )

    for book in batch_books:
        # Prepare embedded branches for this book
        embedded_branches = [
            {
                "_id": primary_branch["_id"],
                "libraryName": primary_branch["name"],
                "location": primary_branch["location"],
                "address": {
                    "street": primary_branch["address"]["street"],
                    "city": primary_branch["address"]["city"],
                    "province": primary_branch["address"]["province"],
                    "postalCode": primary_branch["address"]["postalCode"],
                    "country": primary_branch["address"]["country"]
                },
                "numberOfCopies": random.randint(1, 5)
            }
        ]

        # Add additional random branches
        for branch in additional_branches:
            embedded_branches.append({
                "_id": branch["_id"],
                "libraryName": branch["name"],
                "location": branch["location"],
                "address": {
                    "street": branch["address"]["street"],
                    "city": branch["address"]["city"],
                    "province": branch["address"]["province"],
                    "postalCode": branch["address"]["postalCode"],
                    "country": primary_branch["address"]["country"]
                },
                "numberOfCopies": random.randint(1, 5)
            })

        # Update the book with embedded branches
        books_collection.update_one(
            {"_id": book["_id"]},
            {"$set": {"branches": embedded_branches}}
        )

print("Books updated with branch assignments successfully!")
