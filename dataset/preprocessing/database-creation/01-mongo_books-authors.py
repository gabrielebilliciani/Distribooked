import json
from pymongo import MongoClient
from bson import ObjectId

# MongoDB Connection
client = MongoClient("mongodb://localhost:27017/")  # Adjust if needed
db = client["library"]  # Replace "library" with your desired database name

# Load Dataset
with open("../datasets/final_dataset.json", "r") as f:
    data = json.load(f)

# Maps to Store Pre-Generated IDs
author_ids = {}  # Maps author names to their ObjectId

# Collections
authors_collection = db["authors"]
books_collection = db["books"]

# Step 1: Pre-Generate IDs for Authors
for book in data:
    for author in book["authors"]:
        author_name = author["author_name"]
        if author_name not in author_ids:  # Avoid duplicates
            author_ids[author_name] = ObjectId()  # Generate a unique ID for each author

# Step 2: Insert Authors with Placeholder for Books
for book in data:
    for author in book["authors"]:
        author_name = author["author_name"]
        if not authors_collection.find_one({"_id": author_ids[author_name]}):
            author_doc = {
                "_id": author_ids[author_name],
                "fullName": author_name,
                "yearOfBirth": author.get("year_of_birth"),
                "yearOfDeath": author.get("year_of_death"),
                "avatarUrl": author.get("author_avatar"),
                "about": author.get("author_about"),
                "books": []  # To be updated with embedded book info later
            }
            authors_collection.insert_one(author_doc)

# Step 3: Insert Books and Update Authors with Embedded Book Details
for book in data:
    # Pre-generate the book's ID
    book_id = ObjectId()

    # Prepare the embedded authors for the book
    embedded_authors = [
        {"_id": author_ids[author["author_name"]], "fullName": author["author_name"]}
        for author in book["authors"]
    ]

    # Insert the book into the books collection
    book_doc = {
        "_id": book_id,
        "title": book["title"],
        "subtitle": book["subtitle"],
        "publicationDate": book["issued"],
        "publisher": book["publisher"],
        "language": book["language"],
        "categories": book["categories"],
        "isbn10": book["ISBN 10"],
        "isbn13": book["ISBN 13"],
        "coverImageUrl": book["main_image_url"],
        "authors": embedded_authors,  # Embed author IDs and names here
        "readingsCount": 0 # Initializing the number of readings to 0
    }
    books_collection.insert_one(book_doc)

    # Update each author with the embedded book details
    for author in book["authors"]:
        author_id = author_ids[author["author_name"]]
        other_authors = [
            {"id": author_ids[a["author_name"]], "fullName": a["author_name"]}
            for a in book["authors"]
        ]

        authors_collection.update_one(
            {"_id": author_id},
            {"$addToSet": {
                "books": {
                    "_id": book_id,  # Book ID
                    "title": book["title"],
                    "subtitle": book["subtitle"],
                    "categories": book["categories"],
                    "coverImageUrl": book["main_image_url"],
                    "authors": other_authors  # List co-authors (includes the author himself/herself)
                }
            }}
        )

print("Data successfully inserted!")
