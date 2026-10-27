package com.quizmaster.service;

import com.quizmaster.model.Question;
import com.quizmaster.model.SyllabusUnit;
import java.util.List;

/** A small original sample of questions, one per syllabus area. */
public final class StarterQuestionBank {
    private StarterQuestionBank() { }

    public static List<Question> questions() {
        return List.of(
                new Question("STARTER-U1-001", SyllabusUnit.UNIT_I, "Encapsulation",
                        "Which statement best describes encapsulation in object-oriented programming?",
                        List.of("Combining data with methods and controlling access to that data",
                                "Running several programs at the same time", "Converting source code into machine code",
                                "Storing every object in a global variable"), 0,
                        "Encapsulation keeps an object's state and related behavior together while controlling how the state is accessed."),
                new Question("STARTER-U2-001", SyllabusUnit.UNIT_II, "Objects and constructors",
                        "In Java, what happens when an expression such as `new Book()` is evaluated?",
                        List.of("A Book instance is created and its constructor is invoked",
                                "The Book class is renamed", "Every Book object is deleted", "The program enters a loop"), 0,
                        "The `new` expression creates an instance and invokes the selected constructor to initialize it."),
                new Question("STARTER-U3-001", SyllabusUnit.UNIT_III, "Polymorphism",
                        "For an overridden instance method, which implementation is selected at runtime?",
                        List.of("The implementation belonging to the object's actual class",
                                "Always the implementation in Object", "The first method alphabetically",
                                "A random implementation from the class hierarchy"), 0,
                        "Dynamic method dispatch uses the runtime object's class to select an overridden instance method."),
                new Question("STARTER-U4-001", SyllabusUnit.UNIT_IV, "DDA line algorithm",
                        "In the DDA line scan-conversion algorithm, how are successive x positions commonly computed?",
                        List.of("Add the increment dx/steps to the previous x position",
                                "Multiply x by the polygon area", "Replace x with the line's slope",
                                "Subtract the viewport width on every step"), 0,
                        "DDA divides the line into steps and advances coordinates by dx/steps and dy/steps before rounding to pixels."),
                new Question("STARTER-U5-001", SyllabusUnit.UNIT_V, "2D transformations",
                        "Using column vectors, where do tx and ty appear in the standard 2D homogeneous translation matrix?",
                        List.of("In the last column, above the final homogeneous row",
                                "In the upper-left 2×2 rotation block", "On the main diagonal only",
                                "In the last row as its first two values"), 0,
                        "With column vectors, translation uses [[1,0,tx],[0,1,ty],[0,0,1]], so tx and ty occupy the last column."));
    }
}
