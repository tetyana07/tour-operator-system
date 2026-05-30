package com.touroperator.command;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;


@Component
public class CommandManager {

    private static final int MAX_HISTORY = 30;

    private final Deque<BookingCommand> undoStack = new ArrayDeque<>();
    private final Deque<BookingCommand> redoStack = new ArrayDeque<>();

    private final SimpleBooleanProperty canUndo      = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty canRedo      = new SimpleBooleanProperty(false);
    private final SimpleStringProperty  undoLabel    = new SimpleStringProperty("");
    private final SimpleStringProperty  redoLabel    = new SimpleStringProperty("");


    public void execute(BookingCommand command) {
        command.execute();

        if (command.isUndoable()) {
            if (undoStack.size() >= MAX_HISTORY) undoStack.pollLast();
            undoStack.push(command);
            redoStack.clear();
        }

        updateProperties();
    }

    public void undo() {
        if (undoStack.isEmpty()) throw new IllegalStateException("Немає дій для відміни");
        BookingCommand command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        updateProperties();
    }


    public void redo() {
        if (redoStack.isEmpty()) throw new IllegalStateException("Немає дій для повтору");
        BookingCommand command = redoStack.pop();
        command.execute();
        if (command.isUndoable()) undoStack.push(command);
        updateProperties();
    }


    public void clear() {
        undoStack.clear();
        redoStack.clear();
        updateProperties();
    }


    public int undoSize() { return undoStack.size(); }


    public int redoSize() { return redoStack.size(); }


    public ReadOnlyBooleanProperty canUndoProperty() { return canUndo; }
    public ReadOnlyBooleanProperty canRedoProperty() { return canRedo; }


    public StringProperty undoLabelProperty() { return undoLabel; }


    public StringProperty redoLabelProperty() { return redoLabel; }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }


    private void updateProperties() {
        canUndo.set(!undoStack.isEmpty());
        canRedo.set(!redoStack.isEmpty());
        undoLabel.set(undoStack.isEmpty() ? ""
                : "Скасувати: " + undoStack.peek().describe());
        redoLabel.set(redoStack.isEmpty() ? ""
                : "Повторити: " + redoStack.peek().describe());
    }
}
