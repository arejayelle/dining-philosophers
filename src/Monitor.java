
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Class Monitor
 * To synchronize dining philosophers.
 *
 * @author Serguei A. Mokhov, mokhov@cs.concordia.ca
 */
public class Monitor {

    /*
	 * ------------
	 * Data members
	 * ------------
     */
    //Task 2: Implementation of monitor
    //To handle sync of talking: a boolean indicating if a philosopher is talking
    private boolean aPhilosopherIsTalking = false;
    
    //To handle the food cycle of a philosopher
    private enum Status {full, hungry, hasRightChopstick, eating};
    
    //To hold the status of all the philosophers
    private Status[] state;
    
    //To hold the conditionals of all the philosophers
    private Lock lock = new ReentrantLock();
    private Condition[] chopsticks;
    private Condition talking;
    
    //To hold the number of philosophers at the table
    int nbPhil;

    /**
     * Constructor
     */
    public Monitor(int piNumberOfPhilosophers) {
        //Task 2: Set number of philosophers and initialize data
        nbPhil = piNumberOfPhilosophers;
        state = new Status[nbPhil];
        chopsticks = new Condition[nbPhil];
        talking = lock.newCondition();
        for(int i = 0; i < nbPhil; i++)
        {
            state[i] = Status.full;
            chopsticks[i] = lock.newCondition();
        }
    }

    /*
	 * -------------------------------
	 * User-defined monitor procedures
	 * -------------------------------
     */
    //Task 2: Procedures for getting the left and right philosopher
    private int left(int id)
    {
        return (id > 0) ? id - 1 : nbPhil - 1;
    }
    
    private int right(int id)
    {
        return (id < nbPhil - 1) ? id + 1 : 0;
    }
    /**
     * Task 2:
     * Checks if the philosopher with the given id can
     * pick up the chopsticks and eat
     */
    private void check(int id)
    {
        if(bothChopsticksFree(id)
            && iWantToEat(id))
        {
            state[id] = Status.eating;
        }
        else if(allowedToTakeOneChopstick(id)
                && rightChopstickFree(id)
                && iWantToEat(id))
        {
            state[id] = Status.hasRightChopstick;
        }
    }
    
    //Task 2: Split check into smaller methods
    private boolean bothChopsticksFree(int id)
    {
        return state[left(id)] != Status.eating
            && state[left(id)] != Status.hasRightChopstick
            && state[right(id)] != Status.eating;
    }
    
    private boolean iWantToEat(int id)
    {
        return state[id] == Status.hungry || state[id] == Status.hasRightChopstick;
    }
    
    private boolean allowedToTakeOneChopstick(int id)
    {
        return id % 2 == 0;
    }
    
    private boolean rightChopstickFree(int id)
    {
        return state[right(id)] != Status.eating;
    }
    
    /**
     * Task 2:
     * Grants request (returns) to eat when both chopsticks/forks are available.
     * Else forces the philosopher to wait()
     */
    public void pickUp(final int piTID) {
        //Task2: Implementation of pickUp()
        lock.lock();
        try{
            state[piTID] = Status.hungry;
            check(piTID);
            if(state[piTID] == Status.hungry)
            {
                System.out.println("Philosopher " + (piTID+1) + " is waiting to eat.");
                chopsticks[piTID].await();
            }
            else if (state[piTID] == Status.hasRightChopstick)
            {
                System.out.println("Philosopher " + (piTID + 1) + " has taken the right chopstick");
                chopsticks[piTID].await();
            }
            assert(state[piTID] == Status.eating);
            assert(state[left(piTID)] != Status.eating
                    && state[left(piTID)] != Status.hasRightChopstick);
            assert(state[right(piTID)] != Status.eating);
        }
        catch (InterruptedException e)
        {
            System.err.println("Monitor.pickUp():");
            DiningPhilosophers.reportException(e);
            System.exit(1);
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Task 2:
     * When a given philosopher's done eating, they put the chopstiks/forks down
     * and let others know they are available.
     */
    public void putDown(final int piTID) {
        lock.lock();

        state[piTID] = Status.full;
        
        check(left(piTID));
        if(state[left(piTID)] == Status.eating)
            chopsticks[left(piTID)].signal();
        
        check(right(piTID));
        if(state[right(piTID)] == Status.eating)
            chopsticks[right(piTID)].signal();

        lock.unlock();
    }

    /**
     * Task 2:
     * Only one philosopher at a time is allowed to philosophy (while she is not
     * eating).
     */
    public void requestTalk() {
        //If a philosopher is talking, wait for them to finish, then talk.
        lock.lock();
        try
        {
            if(aPhilosopherIsTalking)
            {
                talking.await();
            }
            aPhilosopherIsTalking = true;
        }
        catch (InterruptedException e)
        {
            System.err.println("Monitor.requestTalk():");
            DiningPhilosophers.reportException(e);
            System.exit(1);
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Task 2:
     * When one philosopher is done talking stuff, others can feel free to start
     * talking.
     */
    public void endTalk() {
        //A philosopher is no longer talking. Notify one waiting philosopher
        //that they can start talking.
        lock.lock();

        aPhilosopherIsTalking = false;
        talking.signal();

        lock.unlock();
    }
}

// EOF
