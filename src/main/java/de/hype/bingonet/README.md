## Bingo Net

The Bingo Net Server is developed in a closed Source Project by [Hype_the_Time](https://github.com/HacktheTime/)

Because the packaging matters this has to be done in a seperate package or the package at the top of the File wouldn't match possibly in Future Projects.

If you have Ideas, want to change things etc please communicate with Hype_the_Time so the Server Side receives the necessary Changes.
- /shared This is mounted 1:1 to the Server Side and should contain all its dependencies in it self. DO NOT USE MINECRAFT IN HERE ANYWHERE!
- /environment This may be used by Shared. This is for a shared package name with different Code. This should only be used if there is really no other way to do it in shared!
- /client Just Client Code.
