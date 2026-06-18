#Name: Joshua Trabka
#Date: 06/05/2026
#Description: Logic for full CRUD performance 

import pandas as pd
from sqlalchemy import create_engine, text, BigInteger, String
import logging

# Configure basic logging for professional monitoring
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

class AnimalShelterCRUD:
    """
    Data Access Object (DAO) class implementing full CRUD operations
    for the AAC Shelter Star Schema Database.
    """
    def __init__(self, user, password, host='localhost', port='3306', db_name='aac_shelter_db'):
        """Initializes the connection engine to the MySQL database."""
        self.connection_string = f"mysql+pymysql://{user}:{password}@{host}:{port}/{db_name}"
        self.engine = create_engine(self.connection_string)
        logging.info("Database CRUD Module initialized successfully.")


    # CREATE OPERATION
    def create_shelter_record(self, animal_id, name, animal_type, breed, color, date_of_birth,
                              outcome_type, outcome_subtype, datetime, sex_upon_outcome,
                              age_upon_outcome, age_upon_outcome_in_weeks, lat, long):
        """
        Inserts a complete transactional record into the Star Schema database,
        safely managing dimension existence and relational constraints.
        """
        try:
            with self.engine.connect() as conn:
                # A. Ensure the animal exists in Dim_Animals
                check_animal = conn.execute(
                    text("SELECT 1 FROM Dim_Animals WHERE animal_id = :animal_id"),
                    {"animal_id": animal_id}
                ).fetchone()

                if not check_animal:
                    conn.execute(
                        text("""
                            INSERT INTO Dim_Animals (animal_id, name, animal_type, breed, color, date_of_birth)
                            VALUES (:animal_id, :name, :animal_type, :breed, :color, :dob)
                        """),
                        {"animal_id": animal_id, "name": name, "animal_type": animal_type, 
                         "breed": breed, "color": color, "dob": date_of_birth}
                    )

                # B. Find or create the outcome ID from Dim_Outcomes
                check_outcome = conn.execute(
                    text("""
                        SELECT outcome_id FROM Dim_Outcomes 
                        WHERE outcome_type = :otype AND outcome_subtype = :osubtype
                    """),
                    {"otype": outcome_type, "osubtype": outcome_subtype}
                ).fetchone()

                if check_outcome:
                    outcome_id = check_outcome[0]
                else:
                    # Insert new combination
                    conn.execute(
                        text("""
                            INSERT INTO Dim_Outcomes (outcome_type, outcome_subtype)
                            VALUES (:otype, :osubtype)
                        """),
                        {"otype": outcome_type, "osubtype": outcome_subtype}
                    )
                    # Fetch the auto-generated ID
                    outcome_id = conn.execute(text("SELECT LAST_INSERT_ID()")).fetchone()[0]

                # C. Insert transaction into Fact_Shelter_Outcomes
                conn.execute(
                    text("""
                        INSERT INTO Fact_Shelter_Outcomes 
                        (animal_id, outcome_id, datetime, age_upon_outcome, age_upon_outcome_in_weeks, sex_upon_outcome, location_lat, location_long)
                        VALUES (:animal_id, :outcome_id, :dt, :age, :age_weeks, :sex, :lat, :long)
                    """),
                    {"animal_id": animal_id, "outcome_id": outcome_id, "dt": datetime, "age": age_upon_outcome,
                     "age_weeks": age_upon_outcome_in_weeks, "sex": sex_upon_outcome, "lat": lat, "long": long}
                )
                conn.commit()
                logging.info(f"Successfully created transaction record for Animal ID: {animal_id}")
                return True
        except Exception as e:
            logging.error(f"Failed to create shelter record: {e}")
            return False


    # 2. READ OPERATION

    def read_shelter_data(self, animal_type_filter=None, limit=5000):
        """
        Executes a high-performance relational JOIN query to reconstruct 
        the dataset into a clean Pandas DataFrame for dashboard rendering.
        """
        try:
            query = """
                SELECT 
                    f.record_id, f.animal_id, a.name, a.animal_type, a.breed, a.color,
                    o.outcome_type, o.outcome_subtype, f.datetime, f.age_upon_outcome_in_weeks,
                    f.sex_upon_outcome, f.location_lat, f.location_long
                FROM Fact_Shelter_Outcomes f
                INNER JOIN Dim_Animals a ON f.animal_id = a.animal_id
                INNER JOIN Dim_Outcomes o ON f.outcome_id = o.outcome_id
            """
            
            params = {}
            if animal_type_filter:
                query += " WHERE a.animal_type = :animal_type"
                params["animal_type"] = animal_type_filter
                
            query += f" ORDER BY f.datetime DESC LIMIT {limit};"
            
            df = pd.read_sql(text(query), con=self.engine, params=params)
            return df
        except Exception as e:
            logging.error(f"Failed to read shelter data: {e}")
            return pd.DataFrame()


    # 3. UPDATE OPERATION

    def update_animal_name(self, animal_id, new_name):
        """Updates an animal's descriptive profile name inside Dim_Animals."""
        try:
            with self.engine.connect() as conn:
                result = conn.execute(
                    text("UPDATE Dim_Animals SET name = :name WHERE animal_id = :animal_id"),
                    {"name": new_name, "animal_id": animal_id}
                )
                conn.commit()
                logging.info(f"Updated name for animal {animal_id} to '{new_name}'. Rows affected: {result.rowcount}")
                return result.rowcount > 0
        except Exception as e:
            logging.error(f"Failed to update animal name: {e}")
            return False


    # 4. DELETE OPERATION

    def delete_fact_record(self, record_id):
        """Removes a specific event record from the Fact log by its Record ID."""
        try:
            with self.engine.connect() as conn:
                result = conn.execute(
                    text("DELETE FROM Fact_Shelter_Outcomes WHERE record_id = :record_id"),
                    {"record_id": record_id}
                )
                conn.commit()
                logging.info(f"Deleted transactional record ID: {record_id}. Rows affected: {result.rowcount}")
                return result.rowcount > 0
        except Exception as e:
            logging.error(f"Failed to delete record {record_id}: {e}")
            return False