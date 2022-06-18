#-----------------------------------------------------------------
# pycparser: cdecl.py
#
# Example of the CDECL tool using pycparser. CDECL "explains" C type
# declarations in plain English.
#
# The AST generated by pycparser from the given declaration is traversed
# recursively to build the explanation. Note that the declaration must be a
# valid external declaration in C. All the types used in it must be defined with
# typedef, or parsing will fail. The definition can be arbitrary - pycparser
# doesn't really care what the type is defined to be, only that it's a type.
#
# For example:
#
# 'typedef int Node; const Node* (*ar)[10];'
# =>
# ar is a pointer to array[10] of pointer to const Node
#
# Copyright (C) 2008-2013, Eli Bendersky
# License: BSD
#-----------------------------------------------------------------
import sys

# This is not required if you've installed pycparser into
# your site-packages/ with setup.py
#
sys.path.extend(['.', '..'])

from pycparser import c_parser, c_ast


def explain_c_declaration(c_decl):
    """ Parses the declaration in c_decl and returns a text
        explanation as a string.

        The last external node of the string is used, to allow
        earlier typedefs for used types.
    """
    parser = c_parser.CParser()

    try:
        node = parser.parse(c_decl, filename='<stdin>')
    except c_parser.ParseError:
        e = sys.exc_info()[1]
        return "Parse error:" + str(e)

    if (not isinstance(node, c_ast.FileAST) or
        not isinstance(node.ext[-1], c_ast.Decl)
        ):
        return "Not a valid declaration"

    return _explain_decl_node(node.ext[-1])


def _explain_decl_node(decl_node):
    """ Receives a c_ast.Decl note and returns its explanation in
        English.
    """
    #~ print decl_node.show()
    storage = ' '.join(decl_node.storage) + ' ' if decl_node.storage else ''

    return (decl_node.name +
            " is a " +
            storage +
            _explain_type(decl_node.type))


def _explain_type(decl):
    """ Recursively explains a type decl node
    """
    typ = type(decl)

    if typ == c_ast.TypeDecl:
        quals = ' '.join(decl.quals) + ' ' if decl.quals else ''
        return quals + _explain_type(decl.type)
    elif typ == c_ast.Typename or typ == c_ast.Decl:
        return _explain_type(decl.type)
    elif typ == c_ast.IdentifierType:
        return ' '.join(decl.names)
    elif typ == c_ast.PtrDecl:
        quals = ' '.join(decl.quals) + ' ' if decl.quals else ''
        return quals + 'pointer to ' + _explain_type(decl.type)
    elif typ == c_ast.ArrayDecl:
        arr = 'array'
        if decl.dim: arr += '[%s]' % decl.dim.value

        return arr + " of " + _explain_type(decl.type)

    elif typ == c_ast.FuncDecl:
        if decl.args:
            params = [_explain_type(param) for param in decl.args.params]
            args = ', '.join(params)
        else:
            args = ''

        return ('function(%s) returning ' % (args) +
                _explain_type(decl.type))


if __name__ == "__main__":
    if len(sys.argv) > 1:
        c_decl  = sys.argv[1]
    else:
        c_decl = "char *(*(**foo[][8])())[];"

    print("Explaining the declaration: " + c_decl + "\n")
    print(explain_c_declaration(c_decl) + "\n")

