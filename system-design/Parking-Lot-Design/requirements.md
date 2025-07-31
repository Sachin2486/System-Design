You are tasked with designing a parking system for a multi-level parking lot. The parking lot can accommodate different types of vehicles, including motorcycles, cars, and buses. The system should efficiently manage the parking process, including vehicle entry, parking slot assignment, and exit.

Requirements
Parking Lot Structure

The parking lot has multiple levels, each with a fixed number of parking slots.
Slots are categorized based on vehicle types:
Motorcycle Slot: Can accommodate motorcycles only.
Car Slot: Can accommodate cars and motorcycles.
Bus Slot: Can accommodate buses, cars, and motorcycles.
Vehicle Types

Motorcycle
Car
Bus
Parking Process

A vehicle enters the parking lot and is assigned an available parking slot based on its type.
If no suitable slot is available, the vehicle cannot enter the parking lot.
The system should minimize the time taken to find a parking slot.
Exit Process

When a vehicle exits, the system should free up the parking slot.
The system should calculate the parking fee based on the vehicle type and the duration of the parking.
Parking Fee

Motorcycle: $1 per hour.
Car: $2 per hour.
Bus: $5 per hour.
Admin Features

The admin can view the status of the parking lot (number of free/occupied slots by type).
The admin can add or remove levels or slots.