#include<bits/stdc++.h>
using namespace std ;

enum class VehicleTypes{
    small,
    medium,
    large,
};

class ParkingSpot{
    private:
    int id;
    VehicleTypes spotType;
    bool isEmpty;

    public:
    ParkingSpot(int id,VehicleTypes spotType,bool isEmpty){
        id = id;
        spotType = spotType;
        isEmpty = isEmpty;
    }

    int getID(){
        return id;
    }

    void setId(int setId){
        id = setId;
    }

    VehicleTypes getSpotType(){
        return spotType;
    }

    void setEmptyStatus(bool emptyStatus){
        isEmpty = emptyStatus;
    }

    bool getEmptyStatus(){
        return isEmpty;
    }
};

class ParkingSpace{
    private:
    vector<ParkingSpot> ParkingSpots;

    public:
    void addSpotinParkingSpace(ParkingSpot spot){
        ParkingSpots.push_back(spot);
    }

    vector<ParkingSpot> getAvailableSpots(VehicleTypes VehicleType){
        vector<ParkingSpot> availableSpot;
        cout<<"Size of the vehicle you want to park is:"<<ParkingSpots.size()<<"\n";
        for(int ind = 0;ind < ParkingSpots.size();ind++){
            if(VehicleType == ParkingSpots[ind].getSpotType() && !ParkingSpots[ind].getEmptyStatus()){
                availableSpot.push_back(ParkingSpots[ind]);

            }
        }
        return availableSpot;
    }

    vector<ParkingSpot> getAllSpotStatus(){
        vector<ParkingSpot> allSpots;
        for(int i=0;i<ParkingSpots.size();i++){
            allSpots.push_back(ParkingSpots[i]);
        }
        return allSpots;
    }
};

class ParkVehicle{
    public:
    void parkVehicle(VehicleTypes vehicle,ParkingSpace space1){
        vector<ParkingSpot> availableSpot = space1.getAvailableSpots(vehicle);
        cout<<"available spot is:"<<availableSpot.size()<<"\n";
        if(availableSpot.size() == 0){
            cout<<"no empty spots are available\n";
            return;
        }
        availableSpot[0].setEmptyStatus(true);
    }

    void freeSpot(VehicleTypes vehicle,ParkingSpace space1){
        vector<ParkingSpot> allSpots = space1.getAllSpotStatus();

        for(int i=0;i<allSpots.size();i++){
            if(allSpots[i].getSpotType() == vehicle){
                cout<<"Empty Space Available for parking\n";
                allSpots[i].setEmptyStatus(false);
                return;
            }
        }
    }

};

int main(){
    ParkingSpace space1;
    ParkingSpot spot1(1,VehicleTypes::medium,false);
    ParkingSpot spot2(2,VehicleTypes::large,false);
    space1.addSpotinParkingSpace(spot1);
    space1.addSpotinParkingSpace(spot2);
    ParkVehicle park1;
    park1.parkVehicle(VehicleTypes::medium,space1);
}
