package com.caps.lexer;

public class PrognToken implements Token{
    @Override
    public boolean equals(final Object other){
        return other instanceof PrognToken;
    }
    @Override
    public int hashCode(){
        return 777;
    }

    @Override
    public String toString() {
        return "PrognToken{}";
    }
}
