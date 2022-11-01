package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {
    // a change so that git works
    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
           for (Ast.Global global : ast.getGlobals()) {
               visit(global);
           }
           for (Ast.Function function : ast.getFunctions()) {
               visit(function);
           }

           return scope.lookupFunction("main", 0).invoke(Collections.emptyList());
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
           if (ast.getValue().isPresent()) scope.defineVariable(ast.getName(), ast.getMutable(), visit(ast.getValue().get()));
           else scope.defineVariable(ast.getName(), ast.getMutable(), Environment.NIL);
           return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) { // this defines functions in current scope
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            try {
                scope = new Scope(scope);

                for (int i = 0; i < args.size(); i++) { // this defines arguments
                    scope.defineVariable(ast.getParameters().get(i), true, args.get(i));
                }

                for (Ast.Statement stmt : ast.getStatements()) {
                    visit(stmt);
                }
            } catch (Return r) {
                return r.value;
            } finally {
                scope = scope.getParent();
            }
            return Environment.NIL;
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {

        if( ast.getValue().isPresent() ) {
            Ast.Expression expr = (Ast.Expression) ast.getValue().get();
            Environment.PlcObject var = new Environment.PlcObject(scope, expr);
            scope.defineVariable(ast.getName(), false, visit(expr));
            //return Environment.create(var);
        }
        else scope.defineVariable(ast.getName(), false, Environment.NIL);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        Ast.Expression acc = ast.getReceiver();
        if (acc.getClass().equals(Ast.Expression.Access.class)) {
            try {
                Ast.Expression.Access postcheck = (Ast.Expression.Access) ast.getReceiver();
                scope = new Scope(scope);
                if (postcheck.getOffset().isPresent()) { // this means it is a list
                    List<Object> list = new ArrayList<>();

                    Ast.Expression.Literal off = (Ast.Expression.Literal) postcheck.getOffset().get();
                    BigInteger offset = (BigInteger) off.getLiteral();


                    Object values = scope.lookupVariable(postcheck.getName()).getValue().getValue();
                    System.out.println(values);
                    list = (List<Object>) values; // TODO: stupid cast issue again...
                    Object futureval = visit(ast.getValue()).getValue();
                    list.set(offset.intValue(), futureval);
                    scope.lookupVariable(postcheck.getName()).setValue(Environment.create(list));

                   // TODO: does this work correctly??? it takes the values, changes them in a local temp list, then sets values to temp list
                }
                else { // this means its just a variable
                    scope.lookupVariable(postcheck.getName()).setValue(visit(ast.getValue()));
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        else throw new RuntimeException("wrong type for assignment");
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        //System.out.println("evaluating if");
        if (requireType(Boolean.class, visit(ast.getCondition())) != null) { // checks to make sure visit both exists and is a boolean
            try {
                scope = new Scope(scope);

                if ((Boolean)visit(ast.getCondition()).getValue()) { // if condition evaluates to true
                    for (int i = 0; i < ast.getThenStatements().size(); i++) {
                        visit(ast.getThenStatements().get(i)); // visit then statements
                    }
                }
                else { // if condition evaluates to false
                    for (int i = 0; i < ast.getElseStatements().size(); i++) {
                        visit(ast.getElseStatements().get(i)); // visit else statements
                    }
                }
            }
            finally {
                scope = scope.getParent(); // restore scope
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {

        System.out.println("evaluating switch");
        for (int i = 0; i < ast.getCases().size(); i++) { // for each didnt work here for some fuckign reason
            //requireType(Ast.Statement.Case.class, visit(ast.getCases().get(i)));
            visit(ast.getCases().get(i));
            //System.out.println(ast.getCases().get(i).getClass());
        }
        //scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {

        //System.out.println(ast.getCases());
        //System.out.println(visit(ast.getCondition()));
//        if (requireType(Boolean.class, visit(ast.getCondition())) != null) { // checks to make sure visit both exists and is a boolean
//            System.out.println("switch if");
//        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        System.out.println("evaluating case");
        //System.out.println(ast.getValue().get());
        for (Ast.Statement stmt : ast.getStatements()) {
            //System.out.println(visit(stmt));
            visit(stmt);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        //throw new UnsupportedOperationException(); //TODO (in lecture)
        //System.out.println("inside while visit");
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
             try {
                 scope = new Scope(scope);
                 for (Ast.Statement stmt : ast.getStatements()) {
                     visit(stmt);
                 }
             }
             finally {
                 scope = scope.getParent();
             }
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() == null) {
            return Environment.NIL;
        }
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {

        String op = ast.getOperator();

        if (op.equals("&&")) {
            // TODO: THIS ONLY WORKS WHEN BOTH EXIST?
            if (requireType(Boolean.class, visit(ast.getLeft())) == requireType(Boolean.class, visit(ast.getRight())))
                return visit(ast.getLeft()); // return left
            else return Environment.create(Boolean.FALSE); // if not return false
        }

        if (op.equals("||")) {
            if (requireType(Boolean.class, visit(ast.getLeft())))
                return visit(ast.getLeft()); // return left
            if (requireType(Boolean.class, visit(ast.getRight())))
                return visit(ast.getRight()); // return right
            else return Environment.create(Boolean.FALSE); // if not return false
        }

        if (op.equals("<")) {
            if (visit(ast.getLeft()).getValue().getClass() == visit(ast.getRight()).getValue().getClass()) {
                Comparable<Object> left = (Comparable<Object>) visit(ast.getLeft()).getValue();
                Comparable<Object> right = (Comparable<Object>) visit(ast.getRight()).getValue();
                int compare = left.compareTo(right);

                if (compare < 0) return Environment.create(Boolean.TRUE);
                else return Environment.create(Boolean.FALSE);
            }
        }

        if (op.equals(">")) {
            if (visit(ast.getLeft()).getValue().getClass() == visit(ast.getRight()).getValue().getClass()) {
                Comparable<Object> left = (Comparable<Object>) visit(ast.getLeft()).getValue();
                Comparable<Object> right = (Comparable<Object>) visit(ast.getRight()).getValue();
                int compare = left.compareTo(right);

                if (compare > 0) return Environment.create(Boolean.TRUE);
                else return Environment.create(Boolean.FALSE);
            }
        }

        if (op.equals("==")) {
            if (visit(ast.getLeft()).getValue().getClass() == visit(ast.getRight()).getValue().getClass()) {
                Comparable<Object> left = (Comparable<Object>) visit(ast.getLeft()).getValue();
                Comparable<Object> right = (Comparable<Object>) visit(ast.getRight()).getValue();
                int compare = left.compareTo(right);

                if (compare == 0) return Environment.create(Boolean.TRUE);
                else return Environment.create(Boolean.FALSE);
            }
        }

        if (op.equals("!=")) {
            if (visit(ast.getLeft()).getValue().getClass() == visit(ast.getRight()).getValue().getClass()) {
                Comparable<Object> left = (Comparable<Object>) visit(ast.getLeft()).getValue();
                Comparable<Object> right = (Comparable<Object>) visit(ast.getRight()).getValue();
                int compare = left.compareTo(right);

                if (compare != 0) return Environment.create(Boolean.TRUE);
                else return Environment.create(Boolean.FALSE);
            }
        }

        if (op.equals("+")) {
            //System.out.println("conc");

            // string
            if (visit(ast.getLeft()).getValue().getClass().equals(String.class) && visit(ast.getRight()).getValue().getClass().equals(String.class)) {
                //System.out.println("two strings");
                return Environment.create(visit(ast.getLeft()).getValue().toString() + visit(ast.getRight()).getValue().toString());
            }
            // bigDecimal
            if (visit(ast.getLeft()).getValue().getClass().equals(BigDecimal.class) && visit(ast.getRight()).getValue().getClass().equals(BigDecimal.class)) {
                //System.out.println("two decimal");
                return Environment.create(BigDecimal.class.cast(visit(ast.getLeft()).getValue()).add(BigDecimal.class.cast(visit(ast.getRight()).getValue())));
            }
            //bigInteger
            if (visit(ast.getLeft()).getValue().getClass().equals(BigInteger.class) && visit(ast.getRight()).getValue().getClass().equals(BigInteger.class)) {
                //System.out.println("two integer");
                return Environment.create(BigInteger.class.cast(visit(ast.getLeft()).getValue()).add(BigInteger.class.cast(visit(ast.getRight()).getValue())));
            }
            else throw new RuntimeException("incorrect concatenation types");

        }

        if (op.equals("-")) {
            //System.out.println("conc");

            // bigDecimal
            if (visit(ast.getLeft()).getValue().getClass().equals(BigDecimal.class) && visit(ast.getRight()).getValue().getClass().equals(BigDecimal.class)) {
                //System.out.println("two decimal");
                return Environment.create(BigDecimal.class.cast(visit(ast.getLeft()).getValue()).subtract(BigDecimal.class.cast(visit(ast.getRight()).getValue())));
            }
            //bigInteger
            if (visit(ast.getLeft()).getValue().getClass().equals(BigInteger.class) && visit(ast.getRight()).getValue().getClass().equals(BigInteger.class)) {
                //System.out.println("two integer");
                return Environment.create(BigInteger.class.cast(visit(ast.getLeft()).getValue()).subtract(BigInteger.class.cast(visit(ast.getRight()).getValue())));
            }
            else throw new RuntimeException("incorrect concatenation types");

        }

        if (op.equals("/")) {
            //System.out.println("conc");
            MathContext mc = new MathContext(1, RoundingMode.HALF_UP) ;
            // bigDecimal
            if (visit(ast.getLeft()).getValue().getClass().equals(BigDecimal.class) && visit(ast.getRight()).getValue().getClass().equals(BigDecimal.class)) {
                if (BigDecimal.class.cast(visit(ast.getRight()).getValue()).equals(BigDecimal.ZERO)) throw new RuntimeException("divide by 0");
                //System.out.println("two decimal");
                return Environment.create(BigDecimal.class.cast(visit(ast.getLeft()).getValue()).divide(BigDecimal.class.cast(visit(ast.getRight()).getValue()), mc));
            }
            //bigInteger
            if (visit(ast.getLeft()).getValue().getClass().equals(BigInteger.class) && visit(ast.getRight()).getValue().getClass().equals(BigInteger.class)) {
                //System.out.println("two integer");
                return Environment.create(BigInteger.class.cast(visit(ast.getLeft()).getValue()).divide(BigInteger.class.cast(visit(ast.getRight()).getValue())));
            }
            else throw new RuntimeException("incorrect division types");

        }

        if (op.equals("*")) {
            //System.out.println("conc");
            MathContext mc = new MathContext(2, RoundingMode.HALF_UP) ;
            // bigDecimal
            if (visit(ast.getLeft()).getValue().getClass().equals(BigDecimal.class) && visit(ast.getRight()).getValue().getClass().equals(BigDecimal.class)) {
                //System.out.println("two decimal");
                return Environment.create(BigDecimal.class.cast(visit(ast.getLeft()).getValue()).multiply(BigDecimal.class.cast(visit(ast.getRight()).getValue()), mc));
            }
            //bigInteger
            if (visit(ast.getLeft()).getValue().getClass().equals(BigInteger.class) && visit(ast.getRight()).getValue().getClass().equals(BigInteger.class)) {
                //System.out.println("two integer");
                return Environment.create(BigInteger.class.cast(visit(ast.getLeft()).getValue()).multiply(BigInteger.class.cast(visit(ast.getRight()).getValue())));
            }
            else throw new RuntimeException("incorrect multiplication types");

        }


        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {

        if (ast.getOffset().isPresent()) { // list
            Ast.Expression.Literal off = (Ast.Expression.Literal) ast.getOffset().get();
            BigInteger offset = (BigInteger) off.getLiteral(); // i did this in two lines as to prevent casting issues
            Environment.PlcObject value = scope.lookupVariable(ast.getName()).getValue();
            Object values = scope.lookupVariable(ast.getName()).getValue().getValue();
            //System.out.println(values);
            List<Object> list = (List<Object>) values;
            return Environment.create(list.get(offset.intValue()));
        }
        return scope.lookupVariable(ast.getName()).getValue();

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        try {
            scope = new Scope(scope);
            List<Environment.PlcObject> args = new ArrayList<>();
            for (Ast.Expression arg : ast.getArguments()) {
                args.add(visit(arg));
            }
            return scope.lookupFunction(ast.getName(), args.size()).invoke(args);
        }
        finally {
            scope = scope.getParent(); // return scope
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        try {
            scope = new Scope(scope);
            List<Object> values = new ArrayList<>();

            for (Ast.Expression val : ast.getValues()) {
                values.add(visit(val).getValue());
            }
            return new Environment.PlcObject(scope, values);
            //System.out.println(values);
        }
        finally {
            scope = scope.getParent(); // return scope
        }
        //return Environment.NIL;
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}