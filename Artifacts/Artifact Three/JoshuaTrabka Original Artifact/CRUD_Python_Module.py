# Example Python Code to Insert a Document 
#Joshua Trabka
#CS - 340
#09/27/2025

from pymongo import MongoClient
from bson.objectid import ObjectId

class AnimalShelter(object):
    """ CRUD operations for Animal collection in MongoDB """

    def __init__(self):
        USER = 'aacuser'
        PASS = 'Passw0rd1!'
        HOST = 'localhost'
        PORT = 27017
        DB = 'aac'
        COL = 'animals'

        self.client = MongoClient(f'mongodb://{USER}:{PASS}@{HOST}:{PORT}')
        self.database = self.client[DB]
        self.collection = self.database[COL]

    def get_next_record_number(self):
        """ Finds the highest record_id and returns the next one. """
        last_record = self.collection.find_one(
            sort=[("record_id", -1)],
            projection={"record_id": 1, "_id": 0}
        )
        if last_record and "record_id" in last_record:
            return last_record["record_id"] + 1
        return 1

    # ---------- C ----------
    def create(self, data):
        if data is not None and isinstance(data, dict):
            data["record_id"] = self.get_next_record_number()
            insertValid = self.collection.insert_one(data)
            return True if insertValid.acknowledged else False
        else:
            raise Exception("Nothing to save, because data parameter is empty")

    # ---------- R ----------
    def getRecordId(self, postId):
        """ Find a record by MongoDB _id """
        try:
            _data = self.collection.find_one({"_id": ObjectId(postId)})
            return _data
        except Exception as e:
            print(f"Error fetching record: {e}")
            return None

    def getRecordCriteria(self, criteria=None):
        """
        Find records by criteria (dict). Excludes _id field by default.
        Example: {"name": "Rex", "age_upon_outcome": "2 months"}
        """
        if criteria is not None:
            _data = self.collection.find(criteria, {"_id": 0})
        else:
            _data = self.collection.find({}, {"_id": 0})

        return list(_data)
    def update(self, query, new_values, many=False):
        """
        Update documents in the collection
        :param query: dictionary (lookup key/value pairs for filtering)
        :param new_values: dictionary (fields to update, wrapped with {"$set": ...})
        :param many: boolean -> True = update_many, False = update_one
        :return: number of documents modified
        """
        if many:
            result = self.collection.update_many(query, new_values)
        else:
            result = self.collection.update_one(query, new_values)
        return result.modified_count

    # DELETE
    def delete(self, query, many=False):
        """
        Delete documents from the collection
        :param query: dictionary (lookup key/value pairs for filtering)
        :param many: boolean -> True = delete_many, False = delete_one
        :return: number of documents deleted
        """
        if many:
            result = self.collection.delete_many(query)
        else:
            result = self.collection.delete_one(query)
        return result.deleted_count
