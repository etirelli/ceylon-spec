package com.redhat.ceylon.compiler.typechecker.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Util {

    /**
     * Is the second scope contained by the first scope?
     */
    public static boolean contains(Scope outer, Scope inner) {
        while (inner!=null) {
            if (inner==outer) {
                return true;
            }
            inner = inner.getContainer();
        }
        return false;
    }
    
    /**
     * Get the class or interface that "this" and "super" 
     * refer to. 
     */
    public static ClassOrInterface getContainingClassOrInterface(Scope scope) {
        while (!(scope instanceof Package)) {
            if (scope instanceof ClassOrInterface) {
                return (ClassOrInterface) scope;
            }
            scope = scope.getContainer();
        }
        return null;
    }
    
    /**
     * Get the class or interface that "outer" refers to. 
     */
    public static ProducedType getOuterClassOrInterface(Scope scope) {
        Boolean foundInner = false;
        while (!(scope instanceof Package)) {
            if (scope instanceof ClassOrInterface) {
                if (foundInner) {
                    return ((ClassOrInterface) scope).getType();
                }
                else {
                    foundInner = true;
                }
            }
            scope = scope.getContainer();
        }
        return null;
    }
    
    /**
     * Convenience method to bind a single type argument 
     * to a toplevel type declaration.  
     */
    public static ProducedType producedType(TypeDeclaration declaration, ProducedType typeArgument) {
        return declaration.getProducedType(null, Collections.singletonList(typeArgument));
    }

    /**
     * Convenience method to bind a list of type arguments
     * to a toplevel type declaration.  
     */
    public static ProducedType producedType(TypeDeclaration declaration, ProducedType... typeArguments) {
        return declaration.getProducedType(null, Arrays.asList(typeArguments));
    }

    static boolean isResolvable(Declaration declaration) {
        return declaration.getName()!=null &&
            !(declaration instanceof Setter) && //return getters, not setters
            !(declaration instanceof Class && 
                    Character.isLowerCase(declaration.getName().charAt(0))); //don't return the type associated with an object dec 
    }
    
    static boolean isNamed(String name, Declaration d) {
        return d.getName()!=null && d.getName().equals(name);
    }
    
    static boolean isNameMatching(String startingWith, Declaration d) {
    	return d.getName()!=null && d.getName().startsWith(startingWith);
    }
    
    /**
     * Produce a map of type parameter to type argument
     * given a list of type arguments to a declaration 
     * and the containing type.
     *  
     * @param declaration a declaration
     * @param declaringType the produced type of which
     *        the declaration is a member
     * @param typeArguments explicit or inferred type 
     *        arguments of the declaration
     */
    static Map<TypeParameter,ProducedType> arguments(Declaration declaration, 
    		ProducedType declaringType, List<ProducedType> typeArguments) {
        Map<TypeParameter, ProducedType> map = new HashMap<TypeParameter, ProducedType>();
        ProducedType dt = declaringType;
        while (dt!=null) {
            map.putAll(dt.getTypeArguments());
            dt = dt.getDeclaringType();
        }
        if (declaration instanceof Generic) {
            Generic g = (Generic) declaration;
            for (int i=0; i<g.getTypeParameters().size(); i++) {
                if (typeArguments.size()>i) {
                    map.put(g.getTypeParameters().get(i), typeArguments.get(i));
                }
            }
        }
        return map;
    }
    
    static <T> List<T> list(List<T> list, T element) {
        List<T> result = new ArrayList<T>();
        result.addAll(list);
        result.add(element);
        return result;
    }

    /**
     * Helper method for eliminating duplicate types from
     * lists of types that form a union type, taking into
     * account that a subtype is a "duplicate" of its
     * supertype.
     */
    public static void addToUnion(List<ProducedType> list, ProducedType pt) {
        if (pt==null) {
            return;
        }
        else if (pt.getDeclaration() instanceof UnionType) {
            for (ProducedType t: pt.getDeclaration().getCaseTypes() ) {
                addToUnion( list, t.substitute(pt.getTypeArguments()) );
            }
        }
        else {
            Boolean included = pt.isWellDefined();
            if (included) {
                for (Iterator<ProducedType> iter = list.iterator(); iter.hasNext();) {
                    ProducedType t = iter.next();
                    if (pt.isSubtypeOf(t)) {
                        included = false;
                        break;
                    }
                    //TODO: I think in some very rare occasions 
                    //      this can cause stack overflows!
                    else if (pt.isSupertypeOf(t)) {
                        iter.remove();
                    }
                }
            }
            if (included) {
                list.add(pt);
            }
        }
    }
    
    /**
     * Helper method for eliminating duplicate types from
     * lists of types that form an intersection type, taking 
     * into account that a supertype is a "duplicate" of its
     * subtype.
     */
    public static void addToIntersection(List<ProducedType> list, ProducedType pt) {
        if (pt==null) {
            return;
        }
        else if (pt.getDeclaration() instanceof IntersectionType) {
            for (ProducedType t: pt.getDeclaration().getSatisfiedTypes() ) {
                addToIntersection( list, t.substitute(pt.getTypeArguments()) );
            }
        }
        else {
            Boolean included = pt.isWellDefined();
            if (included) {
                for (Iterator<ProducedType> iter = list.iterator(); iter.hasNext();) {
                    ProducedType t = iter.next();
                    if (pt.isSupertypeOf(t)) {
                        included = false;
                        break;
                    }
                    //TODO: I think in some very rare occasions 
                    //      this can cause stack overflows!
                    else if (pt.isSubtypeOf(t)) {
                        iter.remove();
                    }
                }
            }
            if (included) {
                list.add(pt);
            }
        }
    }
    
    static String format(List<String> path) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<path.size(); i++) {
            sb.append(path.get(i));
            if (i<path.size()-1) sb.append('.');
        }
        return sb.toString();
    }

    static boolean addToSupertypes(List<ProducedType> list, ProducedType st) {
        for (ProducedType et: list) {
            if (st.isExactly(et)) {
                return false;
            }
        }
        list.add(st);
        return true;
    }

}
