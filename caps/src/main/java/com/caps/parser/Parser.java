package com.caps.parser;

import com.caps.lexer.*;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final  Token[] tokens;

    /*
     * Basic methods 
     */
    public Parser(final Token[] tokens){
        this.tokens = tokens;
    } // Parser

    private Token getToken(final int p) throws ParseException{
        if(p >= 0 && p < tokens.length){
            // valid position
            return tokens[p];
        }else{
            throw new ParseException("out of range");
        }
    } // get token

    public void assertTokenIs (final int p, final Token expected) throws ParseException{
        final Token received = getToken(p);
        if (!expected.equals(received)) {
            throw new ParseException("Expected: " + expected.toString() + "Received: " + received.toString());
        }
    } // assert tokens

    /*
     * Parse Program
     */

    // program ::= statement
    public static Program parseProgram(final Token[] tokens) throws ParseException{
        final Parser parser = new Parser(tokens);
        final ParseResult<Program> program = parser.parseProgram(0);
        if(program.nextP == tokens.length){
            return program.result;
        }else{
            throw new ParseException("remaining tokens at end, starting with : " + parser.getToken(program.nextP).toString());
        }
    }// parse Program

    public ParseResult<Program> parseProgram(final int p) throws ParseException{
        final ParseResult<Stmt> stmt = parseStmt(p);
        return new ParseResult<Program>(new Program(stmt.result), stmt.nextP);
    } // parseProgram

    /*
     * Parse Stmts
     */

    /*
        > stmt ::= exp`;` | 
        > var `IS` exp`;` (vardec) |
        > `RETURNS` exp`;` (returns) |
        > `{` stmt* `}`|  statement blocks
        > `IF` `(` exp `)` stmt `ELSE` `stmt` | if-else statements
        > `WHILE` `(` exp `)` stmt  loop statements

        * stmt :: = vardec | returns | loop | ifelse | block statments |
     */

    public ParseResult<Stmt> parseStmt(final int p) throws ParseException{
        try {
            return parseVardec(p);
        } catch(final ParseException e1){
            try {
                return parseReturns(p);

            }catch(final ParseException e2){
                try{
                    return parseLoop(p);
                }catch(final ParseException e3){
                    try{
                        return parseIf(p);
                    }catch(final ParseException e4){
                        try{
                            return parseStmts(p);
                        }catch(final ParseException e5){
                            try{
                                return parseExpStmt(p);
                            }catch(final ParseException e6){
                                try{
                                    return parseMethodDef2(p);
                                }catch( final ParseException e7){
                                    return parseProgn(p);
                                }
                            }
                        }
                    }
                }
            }
        }
    } // parse Stmt

    // progn ::= `{` `PROGON` statement* `}`
    public ParseResult<Stmt> parseProgn(int p) throws ParseException {
        assertTokenIs(p, new LeftBracketToken());
        assertTokenIs(p + 1, new PrognToken());
        final List<Stmt> stmts = new ArrayList<>();
        boolean flag = true;
        p += 2;
        while(flag){
            try{
                final ParseResult<Stmt> stmt = parseStmt(p);
                stmts.add(stmt.result);
                p = stmt.nextP;
            } catch(final ParseException e){
                flag = false;
            }
        }
        assertTokenIs(p, new RightBracketToken());
        return new ParseResult<Stmt>(new ProgonStmt(stmts), p + 1);
    }

    // parse single stmt
    public ParseResult<Stmt> parseExpStmt(int p) throws ParseException {
        ParseResult<Exp> exp = parseExp(p);
        assertTokenIs(exp.nextP, new SemicolonToken());
        return new ParseResult<Stmt>(new ExpStmt(exp.result), exp.nextP + 1);
    }


    public ParseResult<Stmt> parseStmts(int p) throws ParseException {
        assertTokenIs(p, new LeftBracketToken());
        p ++;
        List<Stmt> stmts = new ArrayList<>();
        boolean flag  = true;
        while(flag){
            try{
                final ParseResult<Stmt> stmt = parseStmt(p);
                stmts.add(stmt.result);
                p = stmt.nextP;
            }catch (final ParseException e){
                flag = false; // we have met a exception , we catch it and not parsing stmts no more
            }
        }
        assertTokenIs(p, new RightBracketToken());
        return new ParseResult<Stmt>(new BlockStmt(stmts), p + 1);
    }


    //`IF` `(` exp `)` stmt `ELSE` `stmt` | if-else statements
    public ParseResult<Stmt> parseIf(final int p) throws ParseException {
        assertTokenIs(p, new IfToken());
        assertTokenIs(p + 1, new LeftParenToken());
        final ParseResult<Exp> exp = parseExp(p + 2);
        assertTokenIs(exp.nextP, new RightParenToken());
        final ParseResult<Stmt> ifBody = parseStmt(exp.nextP + 1);
        assertTokenIs(ifBody.nextP, new ElseToken());
        final ParseResult<Stmt> elseBody = parseStmt(ifBody.nextP + 1);
        return new ParseResult<Stmt>(new IfElseStmt(
            exp.result,ifBody.result,elseBody.result
        ), elseBody.nextP);
    }


    
    public ParseResult<Stmt> parseReturns(final int p) throws ParseException {
        assertTokenIs(p, new ReturnsToken()); // p th token 
        final ParseResult<Exp> exp = parseExp(p + 1); // this is the p + 1 token
        assertTokenIs(exp.nextP, new SemicolonToken()); // exp might take more than 1 tokens then here our semicolon token should be exp.nextP
        return new ParseResult<Stmt>(new ReturnsStmt(exp.result), exp.nextP + 1); // this next p is suppose to be exp.nextP + 1
    } // parse returns stmt

    // loop ::= `WHILE` `(` exp `)` stmt  loop statements
    public ParseResult<Stmt> parseLoop(final int p) throws ParseException {
        assertTokenIs(p, new WhileToken());
        assertTokenIs(p + 1, new LeftParenToken());
        final ParseResult<Exp> guard = parseExp(p + 2);
        assertTokenIs(guard.nextP, new RightParenToken());
        final ParseResult<Stmt> body = parseStmt(guard.nextP + 1);
        return new ParseResult<Stmt>(new WhileStmt(guard.result,body.result), body.nextP);
    } // parse loop

    public ParseResult<Stmt> parseVardec(final int p) throws ParseException {
        final ParseResult<Variable> variable = parseVariable(p);
        assertTokenIs(variable.nextP, new IsToken());
        final ParseResult<Exp> exp = parseExp(variable.nextP + 1);
        assertTokenIs(exp.nextP, new SemicolonToken());
        return new ParseResult<Stmt>(new VardecStmt(variable.result,exp.result),exp.nextP + 1);
    } // parse vardec

    // END OF STMT

    /*
     * Parse Exp
     */
    
    /*
        exp ::= 
        > var | string | number variables, strings, and numbers are expressions
        > exp op exp arithmetic expressions
        > `PRINT` exp prints to the terminal, returns a number
        >    `CALL` exp `(` exps `)` calls higher-order function with parameters
        >    `(` vars `)` `EXECUTES` exp defines higher-order functions
        >    `(` exp `)` parenthesized expressions TODO: ask professor about this. 
     * exp :: = variable
     */

    // exp ::= var | string | number variables, strings, and numbers are expressions
    public ParseResult<Exp> parseExp(final int p) throws ParseException {
        try {
            return parseArithmeticExpressions(p);
        } catch(final ParseException e1){
            try{
                return parseExecuteExp(p);
            } catch(final ParseException e2){
                try{
                    return parsePrint(p);
                } catch(final ParseException e3){
                    try{
                        return parseCallExp(p);
                    } catch(final ParseException e4){
                        return parseExpVar(p);
                    }
                }
            }
        }
    }
    

    // `(` vars `)` `EXECUTES` exp defines higher-order functions
    public ParseResult<Exp> parseExecuteExp(final int p) throws ParseException{
        assertTokenIs(p, new LeftParenToken());
        ParseResult<Variable> var = parseVariable(p + 1);
        assertTokenIs(var.nextP, new RightParenToken());
        assertTokenIs(var.nextP + 1, new ExecutesToken());
        ParseResult<Exp> exp = parseExp(var.nextP + 2);
        return new ParseResult<Exp>(new ExecuteExp(var.result,exp.result), exp.nextP);
    }

    // `CALL` exp `(` exps `)` calls higher-order function with parameters
    public ParseResult<Exp> parseCallExp(int p) throws ParseException {
        assertTokenIs(p, new CallToken());
        p ++;
        final ParseResult<Exp> exp1 = parseExp(p);
        p = exp1.nextP;
        assertTokenIs(exp1.nextP, new LeftParenToken());
        p = p + 1;
        boolean flag = true;
        List<Exp> exps = new ArrayList<>();
        while(flag){
            try{
                final ParseResult<Exp> expCurr = parseExp(p);
                exps.add(expCurr.result);
                p = expCurr.nextP;
            }catch (final ParseException e){
                flag = false; // we have met a exception , we catch it and not parsing stmts no more
            }
        }
        assertTokenIs(p++, new RightParenToken());
        return new ParseResult<Exp>(new CallExp(exp1.result,exps), p);
    }

    //`PRINT` exp prints to the terminal, returns a number
    public ParseResult<Exp> parsePrint(final int p) throws ParseException {
        assertTokenIs(p, new PrintToken());
        final ParseResult<Exp> exp = parseExp(p + 1);
        return new ParseResult<Exp>(new PrintExp(exp.result), exp.nextP);
    }

    //`(` exp `)`
    public ParseResult<Exp> parseArithmeticExpressions(final int p) throws ParseException{
        assertTokenIs(p, new LeftParenToken());
        ParseResult<Exp> exp1 = parseExp(p + 1);
        ParseResult<Op> op = parseOp(exp1.nextP); // assume op is going to be parsed correcttly for now 
        ParseResult<Exp> exp2 = parseExp(op.nextP);
        assertTokenIs(exp2.nextP, new RightParenToken());
        return new ParseResult<Exp>(new ArithmeticExp(exp1.result,op.result,exp2.result), exp2.nextP + 1);
    }

    // exp :: = var | string | number
    public ParseResult<Exp> parseExpVar(final int p) throws ParseException{
        final Token token = getToken(p);
        if (token instanceof IdentifierToken) {
            ParseResult<Variable> var = parseVariable(p);
            return new ParseResult<Exp>(new VariableExp(var.result),var.nextP);
        } else if (token instanceof StrToken) {
            return new ParseResult<Exp>(new StringExp(((StringToken) token).value), p + 1);
        } else if (token instanceof IntToken) {
            return new ParseResult<Exp>(new NumberLiteralExp(((IntToken) token).value), p + 1);
        } else {
            throw new ParseException("Expected variable; received: " + token.toString());
        }
    }
    


    // END OF EXP

    /*
     * Parse Variable
     */
    public ParseResult<Variable> parseVariable(final int p) throws ParseException {
         
        try{
            return parseVars(p);
        }catch(ParseException e1){
            return parseVar(p);
        }
    }

    //vars ::= {var (`,` var)* } list of variables
    public ParseResult<Variable> parseVars(int p) throws ParseException {
        assertTokenIs(p, new LeftBracketToken());
        p ++;
        boolean flag = true;
        List<Variable> vars = new ArrayList<>();
        while(flag){
            try{
                ParseResult<Variable> varCurr = parseVar(p);
                vars.add(varCurr.result);
                p = varCurr.nextP;
                assertTokenIs(varCurr.nextP, new CommaToken());
                p = varCurr.nextP + 1;
            }catch(ParseException e){
                flag = false;

            }

        }
        assertTokenIs(p, new RightBracketToken());
        p ++;
        return new ParseResult<Variable>(new BlockVar(vars), p);
    }


    // var is a variable
    public ParseResult<Variable> parseVar(final int p) throws ParseException {
        final Token token = getToken(p);
        if (token instanceof IdentifierToken) {
            return new ParseResult<Variable>(new SingleVariable(((IdentifierToken)token).name),
                    p + 1);
        } else {
            throw new ParseException("Expected variable; received: " + token.toString());
        }
    }
    // END OF VARIABLE


    /*
    * Parse Type
    */

    // type::= `STRING` | `NUMBER` | `BOOLEAN`
    public ParseResult<Type> parseType(final int p) throws ParseException {
        final Token token = getToken(p);
        if (token instanceof NumberToken) {
            return new ParseResult<Type>(new NumberType(), p + 1);
        } else if (token instanceof BooleanToken) {
            return new ParseResult<Type>(new BoolType(), p + 1);
        } else if (token instanceof StrToken) {
            return new ParseResult<Type>(new StrType(), p + 1);
        } else {
            throw new ParseException("Expected type; received: " + token);
        }
    }

    // types::= {type (`,` type)*} list of types
    public ParseResult<Type> parseTypes(int p) throws ParseException {
        assertTokenIs(p, new LeftBracketToken());
        p ++;
        boolean flag = true;
        List<Type> types = new ArrayList<>();
        while(flag){
            try{
                ParseResult<Type> typeCurr = parseType(p);
                types.add(typeCurr.result);
                p = typeCurr.nextP;
                assertTokenIs(typeCurr.nextP, new CommaToken());
                p = typeCurr.nextP + 1;
            }catch(ParseException e){
                flag = false;
            }
        }
        assertTokenIs(p, new RightBracketToken());
        p ++;
        return new ParseResult<Type>(new BlockType(types), p);
    }
    // END OF TYPES


    /*
    * Parse Parameter
    */


    public ParseResult<Parameter> parseParameter(final int p) throws ParseException {
        try{
            return parseParams(p);
        }catch(ParseException e1){
            return parseParam(p);
        }
    }

    // param::= type var
    public ParseResult<Parameter> parseParam(final int p) throws ParseException {
        ParseResult<Type> type = parseType(p);
        ParseResult<Variable> var = parseVariable(type.nextP);
        return new ParseResult<Parameter>(new Param(type.result, var.result), var.nextP);
    }

   
    // params ::= {param (`,` param)* }list of parameters
    public ParseResult<Parameter> parseParams (int p) throws ParseException {
        assertTokenIs(p, new LeftBracketToken());
        p ++;
        boolean flag = true;
        List<Parameter> params = new ArrayList<>();
        while (flag) {
            try {
                ParseResult<Parameter> paramCurr = parseParameter(p);
                params.add(paramCurr.result);
                p = paramCurr.nextP;
                assertTokenIs(paramCurr.nextP, new CommaToken());
                p = paramCurr.nextP + 1;
                
            } catch (ParseException e) {
                flag = false;
            }
        }
        assertTokenIs(p++, new RightBracketToken());
        return new ParseResult<Parameter>(new BlockParam(params), p);
    }
    // END OF PARAMETERS




    // MethodDef ::= `DEFINE` type methodName `(` params `)` `{` stmt* `}`
    public ParseResult<Stmt> parseMethodDef2(final int p) throws ParseException {
        assertTokenIs(p, new DefineToken());
        final ParseResult<Type> type = parseType(p + 1);
        final ParseResult<Variable> methodName = parseVariable(type.nextP);
        assertTokenIs(methodName.nextP, new LeftParenToken());
        final ParseResult<Parameter> params = parseParameter(methodName.nextP + 1);
        assertTokenIs(params.nextP, new RightParenToken());
        assertTokenIs(params.nextP + 1, new LeftBracketToken());
        final ParseResult<Stmt> stmt = parseStmt(params.nextP + 2);
        assertTokenIs(stmt.nextP, new RightBracketToken());

        return new ParseResult<Stmt>(new MethodDefStmt(type.result, methodName.result,
                params.result, stmt.result), stmt.nextP + 1);
    } // parse MethodDef








    public ParseResult<Op> parseOp (final int p) throws ParseException {
        final Token token = getToken(p);
        Op op = null;

        if (token instanceof PlusToken) {op = new PlusOp();}
        else if (token instanceof MinusToken) {op = new MinusOp();}
        else if (token instanceof AsteriskToken) {op = new MultOp();}
        else if (token instanceof ForwardSlashToken) {op = new DivideOp();}
        else if (token instanceof LogicalAndToken) {op = new LogicalAndOp();}
        else if (token instanceof LogicalOrToken) {op = new LogicalOrOp();}
        else if (token instanceof GreaterThanToken) {op = new GreaterThanOp();}
        else if (token instanceof LessThanToken) {op = new LessThanOp();}
        else {
            throw new ParseException("Expected operator; received: " + token);
        }

        assert(op != null);
        return new ParseResult<Op>(op, p + 1);

    }




    }


