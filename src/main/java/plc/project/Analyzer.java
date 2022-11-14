package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
       try {
           if (ast.getValue().isPresent()) {
               visit(ast.getValue().get());
               requireAssignable(Environment.getType(ast.getTypeName()), ast.getValue().get().getType());
               scope.defineVariable(ast.getName(), ast.getName(), ast.getValue().get().getType(), ast.getMutable(), Environment.NIL);
               ast.setVariable(scope.lookupVariable(ast.getName()));
           }
           else {
               scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), ast.getMutable(), Environment.NIL);
               ast.setVariable(scope.lookupVariable(ast.getName()));
           }
       }
       catch (RuntimeException e) {
           throw new RuntimeException(e.getMessage());
       }
       return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("Expression statement must be a function call.");
        }
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        try{

            if (!ast.getValue().isPresent() && !ast.getTypeName().isPresent()) {
                throw new RuntimeException("Declaration must have a type or an initial value.");
            }

            Environment.Type type = null;

            if (ast.getTypeName().isPresent()) {
               type = Environment.getType(ast.getTypeName().get());
            }
            if (ast.getValue().isPresent()) {
                visit(ast.getValue().get());
                if (type == null) {
                    type = ast.getValue().get().getType();
                }
                requireAssignable(type, ast.getValue().get().getType());
            }


            Environment.Variable var = scope.defineVariable(ast.getName(), ast.getName(), type, ast.getVariable().getMutable(), Environment.NIL);
            ast.setVariable(var);
        }
        catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
           throw new RuntimeException("expected access expression");
        }
        visit(ast.getReceiver());
        visit(ast.getValue());

        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        try {
            visit(ast.getCondition());
            if (ast.getThenStatements().isEmpty()) throw new RuntimeException("expected then statements");

            requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

            for (Ast.Statement then : ast.getThenStatements()) {
                try {
                    scope = new Scope(scope);
                    visit(then);
                } finally {
                    scope = scope.getParent();
                }
            }
            if (!ast.getElseStatements().isEmpty()) {
                for (Ast.Statement elseStmt : ast.getElseStatements()) {
                    try {
                        scope = new Scope(scope);
                        visit(elseStmt);
                    } finally {
                        scope = scope.getParent();
                    }
                }
            }
        }
        catch (RuntimeException e) {
            throw new RuntimeException();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        if (ast.getValue().getType().toString().equals(this.function.getReturnTypeName().toString())) {
            throw new RuntimeException("expected return type " + this.function.getReturnTypeName() + " but got " + ast.getValue().getType());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object literal = ast.getLiteral();

        if (literal == null) ast.setType(Environment.Type.NIL);
        else if (literal instanceof Boolean) ast.setType(Environment.Type.BOOLEAN);

        else if (literal instanceof Character) ast.setType(Environment.Type.CHARACTER);
        else if (literal instanceof String) ast.setType(Environment.Type.STRING);

        else if (literal instanceof BigInteger) {
            //you have to check for size on these
            BigInteger value  = (BigInteger) literal;
            if (value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0 && value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0) {
                ast.setType(Environment.Type.INTEGER);
            } else {
                throw new RuntimeException("integer literal out of range of int");
            }
        }
        else if (literal instanceof BigDecimal) {
            //you have to check for size on these
            BigDecimal value = (BigDecimal) literal;
            if (value.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) <= 0 && value.compareTo(BigDecimal.valueOf(Double.MIN_VALUE)) >= 0) {
                ast.setType(Environment.Type.INTEGER);
            } else {
                throw new RuntimeException("decimal literal out of range of Double");
            }
        }

        else throw new RuntimeException("unknown literal type");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
      throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        //TODO: COMPLETE IMPLEMENTATION OF THIS
      ast.setVariable(scope.lookupVariable(ast.getName()));
      return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        try {
            if (ast.getArguments().isEmpty()) {
                //TODO: this is method it should not throw an exception
                //throw new RuntimeException("expected arguments");


            }
            Environment.Function func = scope.lookupFunction(ast.getName(), ast.getArguments().size());

            List<Ast.Expression> args = ast.getArguments();
            List<Environment.Type> argTypes = func.getParameterTypes();

            for (int i = 0; i < args.size(); i++) {
                Ast.Expression arg = args.get(i);
                Environment.Type argType = argTypes.get(i);
                visit(arg);
                requireAssignable(argType, arg.getType());
            }

            ast.setFunction(func);
        }
        catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (type != target && target != Environment.Type.ANY && target != Environment.Type.COMPARABLE) {
            throw new RuntimeException("Expected type " + target + ", received " + type + ".");
        }
    }

}
