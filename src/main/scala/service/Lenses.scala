//package service
//
//import shapeless._
//
//object LensSpec {
//  val threadIdLens = lens[Thread] >> 'threadId
//  val postsLens = lens[Thread] >> 'posts
//  val contentLens = lens[Post] >> 'content
//  val postsOfThreadLens = threadIdLens ~ postsLens
//}