//package com.ibm.plain
//
//package preparation
//
//import json.Json
//
//object Modules {
//
//  def test = {
//
//    val removeprogram = "removeChildren.exe <input-file> <output-file>"
//
//    val inputfile = """
//{
//    "root-filepath": "C_1.CATProduct",
//    "commands": [
//        {
//            "command": "remove",
//            "parameters": {
//                "parent-document": "C_1.CATProduct",
//                "child-document": "E_1.CATPart",
//                "position": [
//                    1,
//                    0,
//                    0
//                ]
//            },
//            "command": "insert",
//            "parameters": {
//                "parent-document": "C_1.CATProduct",
//                "child-document": "B_1.CATPart",
//                "position": [
//                    1,
//                    0,
//                    0
//                ]
//            }
//        },
//        {}
//    ]
//}"""
//
//    println(inputfile)
//    val json = Json.parse(inputfile).asObject
//    println(json)
//    val newjson = Map("old-input" -> json)
//    println(Json.build(newjson))
//
//    val outputfile = """
//{
//		"return-code" : 0, 
//		"commands" : [
//			{ "success" : true, "message" : null }
//    ]
//}
//"""
//    println(Json.parse(outputfile))
//
//  }
//}      
