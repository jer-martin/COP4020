package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
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
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        //throw new UnsupportedOperationException(); //TODO
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
        //throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
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
        //throw new UnsupportedOperationException(); //TODO
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() == null) {
            return Environment.NIL;
        }
        return Environment.create(ast.getLiteral());
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        //throw new UnsupportedOperationException(); //TODO
        return visit(ast.getExpression());
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        //throw new UnsupportedOperationException(); //TODO


        String op = ast.getOperator();

        if (op.equals("&&")) {
            if (requireType(Boolean.class, visit(ast.getLeft())) == requireType(Boolean.class, visit(ast.getRight()))) // if types match
                return visit(ast.getLeft()); // return left
            else return Environment.create(Boolean.FALSE); // if not return false
        }


        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        //throw new UnsupportedOperationException(); //TODO

        if (ast.getOffset().isPresent()) {
            return Environment.create(ast.getOffset());
        }
        return scope.lookupVariable(ast.getName()).getValue();

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        //throw new UnsupportedOperationException(); //TODO
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
        throw new UnsupportedOperationException(); //TODO
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