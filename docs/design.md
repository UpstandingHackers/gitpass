git directory has the following subdirectories

 - keys
 - indirect
 - objects
 - inbox

## keys
keys contains one file per user, each named after the "user name"
(default "default"). Each file contains a salt and a cryptobox,
where the key is derived from `scrypt(salt, password)`

Further, each keyfile also contains an associated public key that is 
accessible without decryption; this can be used with the inbox 
mechanism.

## objects

Each object is identified by a unique key. The contents are 
distributed across a set of change files within the object directory.
Each object, regardless of type, consists of a set of key/value pairs; 
each key may be associated with multiple values.
 
Types of objects include:

### groups
A group is a container holding a list of references, each with a name and a key.
 
### Reference replacements
Contains a request to replace a previous reference with a new one; for example, 
to revoke a user's access. Note that these are always a simple change with no 
previous head records, and references are never taken to them. 
 
### documents
The leaf nodes of the system; contains, for example, a username/password combo 
for a website.

## change files
There exist several types of change files, but each one is encrypted 
with the object key and contained within a secret box container. 

### simple change:
Contains at most one previous head record and one or more edit commands

### complex merge change:
Contains one or more previous head records, a merge base record, an ordered 
list of any number of change reference records, and any number of additional 
change records    

### Simple merge:
Contains two or more previous head records. Equivalent to a complex merge 
with merge base equal to the most recent common ancestor of all previous heads, 
and a change reference list equal to a depth-first search of the change history
with each record only included once (the first time that it is seen).

### Snapshot:
Contains exactly one previous head record and a list of `add` change records.
These `ADD` records must produce exactly the same object as the history to date. 

## indirection
`indirect` contains indirections, which are files with names of 
the form `<id>.<client>.<nonce>`, indicating what the client thinks 
is the latest value of the indirection. If more than one file exists 
for a given ID, the client should attempt to merge them, and if this 
is not possible, display the conflict for the user to resolve. 
Whenever merges occur (after any resulting conflicts are resolved), 
a new merge object SHOULD be written indicating the resolution of the 
merge, and a new indirection SHOULD be written to refer to that new 
merge object. Then, any previous indirections for that ID MUST be deleted.

The contents of an indirection are a secret box container containing the 
OID of the head change record for the object. 
