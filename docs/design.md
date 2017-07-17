git directory has the following subdirectories

 - keys
 - indirect
 - objects

keys contains one file per user, each named after the "user name"
(default "default"). The contents contain a salt and a cryptobox,
where the key is derived from scrypt(salt, password)

`indirect` contains indirections, which are files with names of 
the form `<id>.<client>.<nonce>`, indicating what the client thinks 
is the latest value of the indirection. If more than one file exists 
for a given ID, the client should attempt to merge them, and if this 
is not possible, display the conflict for the user to resolve. 
Whenever merges occur (after any resulting conflicts are resolved), 
a new indirection SHOULD be written with the results of that merge 
and any previous indirections for that ID SHOULD be deleted. 
