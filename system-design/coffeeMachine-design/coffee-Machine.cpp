#include<bits/stdc++.h>
using namespace std;

enum class CoffeeType{
    Latte,
    Capachino,
    Espresso,
};

enum class CoffeeSize{
    Large,
    Medium,
    Small,
};

class Coffee{
    private:
    CoffeeType type;
    CoffeeSize size;
    double price;
    
    public:
    Coffee(CoffeeType type, CoffeeSize size, double price)
    : type(type) , size(size) , price(price) {}

    CoffeeType getCoffeeType() const{
        return type;
    }

    CoffeeSize getCoffeeSize() const{
        return size;
    }

    double getPrice() const{
        return price;
    }
};

class CoffeeMachineState 
{
    public:
    virtual void displayOptions() = 0;
    virtual void selectCoffee(CoffeeType type,CoffeeSize size) = 0;
    virtual void brewCoffee() = 0;
    virtual void cancel() = 0;
};

class ReadyState : public CoffeeMachineState
{
    public:
    void displayOptions() override
    {
        cout<<"Welcome to the Coffee Machine"<<"\n";
        cout<<"Please Select from the options:"<<"\n";
        cout << "1. Espresso\n 2. Latte\n 3. Cappuccino\n";
    }

    void selectCoffee(CoffeeType type,CoffeeSize size) override
    {
        cout<<"Selected Coffee!";
        switch (type)
        {
        case CoffeeType :: Capachino:
            cout << "Enjoy your Capachino ,";
            break;
        case CoffeeType :: Espresso:
            cout << "Enjoy your Espresso ,";
            break;
        case CoffeeType :: Latte:
            cout << "Enjoy your Latte ,";
            break;
        default:
            break; 
        }

        switch (size)
        {
        case CoffeeSize ::Large:
            cout << "Size : Large\n";
            break;
        case CoffeeSize ::Medium:
            cout << "Size : Medium\n";
            break;
        case CoffeeSize ::Small:
            cout << "Size : Small";
            break;
        default:
            break;
        }
    }

    void brewCoffee() override
    {
        cout<<"Brewing your coffee...\n";
    }

    void cancel() override
    {
        cout<<"Cancelling your selection...\n";
    }
};

class CoffeeMachine{
    private:
    CoffeeMachineState *currentState;

    public:
    CoffeeMachine() : currentState(new ReadyState()) {}

    ~CoffeeMachine()
    {
        delete currentState;
    }

    void setState(CoffeeMachineState *state)
    {
        delete currentState;
        currentState = state;
    }

    void displayOptions()
    {
        currentState -> displayOptions();
    }

    void selectCoffee(CoffeeType type, CoffeeSize size)
    {
        currentState -> selectCoffee(type,size);
    }

    void brewCoffee()
    {
        currentState -> brewCoffee();
    }

    void cancel()
    {
        currentState -> cancel();
    }

};

int main(){
    CoffeeMachine coffeeMachine;

    coffeeMachine.displayOptions();

    coffeeMachine.selectCoffee(CoffeeType::Capachino, CoffeeSize::Medium);

    coffeeMachine.brewCoffee();

    coffeeMachine.cancel();

    return 0;

};