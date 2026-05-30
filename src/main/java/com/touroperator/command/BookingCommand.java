package com.touroperator.command;


public interface BookingCommand {


    void execute();


    void undo();


    String describe();


    boolean isUndoable();
}
