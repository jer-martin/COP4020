package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
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
    private Environment.Type retType;
    private Environment.Type conditionType;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        if (scope.lookupFunction("main", 0) != null  && scope.lookupFunction("main", 0).getReturnType() == Environment.Type.INTEGER) {
           for (Ast.Global global : ast.getGlobals()) {
              visit(global);
           }
           for (Ast.Function function : ast.getFunctions()) {
              visit(function);
           }
        }
        else {
            throw new RuntimeException("The function main/0 is not defined in this scope.");
        }
        return null;
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


        try {
            List<Environment.Type> paramTypes = new ArrayList<>();
            if (!ast.getParameterTypeNames().isEmpty()) {
                ast.getParameterTypeNames().forEach(type -> paramTypes.add(Environment.getType(type))); // this obviously collects paramtypes and adds them to a list
            }

            if (ast.getReturnTypeName().isPresent()) {
                retType = Environment.getType(ast.getReturnTypeName().get());
            }
            else {
                retType = Environment.Type.NIL;
            }

            scope.defineFunction(ast.getName(), ast.getName(), paramTypes, retType, args -> Environment.NIL);

            if (!ast.getStatements().isEmpty()) {
                for (Ast.Statement statement : ast.getStatements()) {
                    try {
                        scope = new Scope(scope);
                        visit(statement);
                    }
                    finally {
                        scope = scope.getParent();
                    }
                }
            }

            ast.setFunction(scope.lookupFunction(ast.getName(), ast.getParameterTypeNames().size()));

        }
        catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
            //System.out.println("caught an error: " + e.getMessage());
        }

        return null;
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
            //System.out.println("do we get here at all lol");
            Environment.Variable var = scope.defineVariable(ast.getName(), ast.getName(),type,  true, Environment.NIL);
            //System.out.println("post define");
            //System.out.println(scope.lookupVariable(ast.getName()));
            ast.setVariable(var);

        }
        catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
            //System.out.println("caught an error");
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
        visit(ast.getCondition());
        conditionType = ast.getCondition().getType(); // this is the type of the condition, i made it global so that I can access it easily from case
        try {
           for (Ast.Statement.Case caseStmt : ast.getCases()) {
               visit(caseStmt);
           }
       }
       catch (RuntimeException e) {
           throw new RuntimeException(e.getMessage());
       }
       return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        if (ast.getStatements().isEmpty()) throw new RuntimeException("expected statements");
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            if (!ast.getValue().get().getType().getJvmName().equals(conditionType.getJvmName())) throw new RuntimeException("case type does not match condition type");
        }
        for (Ast.Statement s : ast.getStatements()) {
            visit(s);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        try {
            visit(ast.getCondition());
            requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
            try {
                scope = new Scope(scope);
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
       try {
           visit(ast.getValue());
           requireAssignable(retType, ast.getValue().getType());
       }
       catch (RuntimeException e) {
           throw new RuntimeException(e.getMessage());
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
                //System.out.println("type set to integer: " + ast.getType().getName());
            } else {
                throw new RuntimeException("integer literal out of range of int");
            }
        }
        else if (literal instanceof BigDecimal) {
            //you have to check for size on these
            BigDecimal value = (BigDecimal) literal;
            if (value.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) <= 0 && value.compareTo(BigDecimal.valueOf(Double.MIN_VALUE)) >= 0) {
                ast.setType(Environment.Type.DECIMAL);
                //System.out.println("type set to integer: " + ast.getType().getName());
            } else {
                throw new RuntimeException("decimal literal out of range of Double");
            }
        }

        else throw new RuntimeException("unknown literal type");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        if (ast.getExpression() instanceof  Ast.Expression.Binary) {
            visit(ast.getExpression());
            ast.setType(ast.getExpression().getType());
            return null;
        }
        throw new RuntimeException("expected binary expression");
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        try {
            Ast.Expression left = ast.getLeft();
            visit(left);
            Ast.Expression right = ast.getRight();
            visit(right);
            String op = ast.getOperator();

            if (op.equals("&&") || op.equals("||")) {
                requireAssignable(Environment.Type.BOOLEAN, left.getType());
                requireAssignable(Environment.Type.BOOLEAN, right.getType());
                ast.setType(Environment.Type.BOOLEAN);
            }
            else if (op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=") || op.equals("==") || op.equals("!=")) {
                requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
                requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
                ast.setType(Environment.Type.BOOLEAN);
            }
            else if (op.equals("+")) {
                // do the normal number stuff but also add string concatenation
                if (left.getType().equals(Environment.Type.STRING) || right.getType().equals(Environment.Type.STRING)) {
//                    requireAssignable(Environment.Type.STRING, left.getType());
//                    requireAssignable(Environment.Type.STRING, right.getType());
                    ast.setType(Environment.Type.STRING);
                }
                else if (left.getType().equals(Environment.Type.DECIMAL) || right.getType().equals(Environment.Type.DECIMAL)) {
                    requireAssignable(Environment.Type.DECIMAL, left.getType());
                    requireAssignable(Environment.Type.DECIMAL, right.getType());
                    ast.setType(Environment.Type.DECIMAL);
                }
                else if (left.getType().equals(Environment.Type.INTEGER) || right.getType().equals(Environment.Type.INTEGER)) {
                    requireAssignable(Environment.Type.INTEGER, left.getType());
                    requireAssignable(Environment.Type.INTEGER, right.getType());
                    ast.setType(Environment.Type.INTEGER);
                }
                else {
                    throw new RuntimeException("invalid types for addition");
                }
            }
            else if (op.equals("-") || op.equals("*") || op.equals("/")) {
                if (left.getType().equals(Environment.Type.DECIMAL) || left.getType().equals(Environment.Type.INTEGER)) {
                   ast.setType(left.getType());
                }
            }
            else {
                throw new RuntimeException("unknown binary operator");
            }

        }
        catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {

        if (ast.getOffset().isPresent()) { // is global
            visit(ast.getOffset().get());
            ast.setVariable(ast.getOffset().get().getType().getGlobal(ast.getName()));
        }
        else { // isnt global
            ast.setVariable(scope.lookupVariable(ast.getName()));
        }
      ast.setVariable(scope.lookupVariable(ast.getName()));
      return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        try {
            if (ast.getArguments().isEmpty()) {
                //throw new RuntimeException("expected arguments");
                ast.setFunction(scope.lookupFunction(ast.getName(), 0));

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
        //System.out.println("visiting list");
        ast.getValues().forEach(value -> {
            visit(value);
        });

        //System.out.println(ast.getType());
        return null;
        //TODO: write a test for this and implement it
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (type != target && target != Environment.Type.ANY && target != Environment.Type.COMPARABLE) {
            throw new RuntimeException("Expected type " + target + ", received " + type + ".");
        }
    }

}
