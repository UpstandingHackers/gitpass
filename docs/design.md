# Requirements

* A shallow clone will reference all data values necessary to read every active value.
* Full validation may require complete history
* Multiple users, with shared data between users (both passwords and groups can be shared)
* Revocation of access to future updates (by cloning history under a new key)

# Structure

git directory has the following subdirectories

 - keys
 - objects
 - grants

## keys
keys contains one file per user, each named after the "user name"
(default "default"). Each file contains a salt and a cryptobox,
where the key is derived from `scrypt(salt, password)`

Further, each keyfile also contains an associated public key that is 
accessible without decryption; this can be used with the granting 
mechanism.

## objects

The object directory contain a list of directories, each one named by a UUID 
in raw hex format. This directory contains  
Each object consists of one or more key-value pairs, identified by a unique 
cryptographic key (which is not one of those KV pairs). The contents of an 
object are derived by applying all fragments within the objects directory 
that apply to the object in chronological order (fragment form a linked 
timestamp chain). Each key may be associated with multiple values.
 
Types of objects include:

### groups
A group is a container holding a list of references.
 
There are two types of properties:

Metadata properties begin with a `.`. The only one currently defined is:

`.name`: The name of this group. It contains a string

Others may be defined in the future.

Member properties are named `/<uuid>`; the value contains a bare key.


 
### Reference replacements
Contains a request to replace a previous reference with a new one; for example, 
to revoke a user's access. Note that these are always a simple change with no 
previous head records, and references are never taken to them.
 
### documents
The leaf nodes of the system; contains, for example, a username/password combo 
for a website.

## fragments
There exist several types of fragments, but each one is encrypted 
with the object key and contained within a secret box container. 

### simple change:
Contains at most one previous head record and one or more edit commands

### complex merge change:
Contains one or more previous head records, a merge base record, an ordered 
list of any number of fragment reference records, and any number of additional 
change records    

### Simple merge:
Contains two or more previous head records. Equivalent to a complex merge 
with merge base equal to the most recent common ancestor of all previous heads, 
and a fragment reference list equal to a depth-first search of the fragment history
with each record only included once (the first time that it is seen).

### Snapshot:
Contains exactly one previous head record and a dictionary of the current state 

## indirection
Each object also contains at least one or more indirections, which are files with 
names of the form `indir.<client>.<nonce>`, indicating what the client thinks is 
the latest value of the indirection. If more than one file exists for a given ID, 
the client should attempt to merge them, and if this is not possible, display the 
conflict for the user to resolve. Whenever merges occur (after any resulting 
conflicts are resolved), a new merge object SHOULD be written indicating the 
resolution of the merge, and a new indirection SHOULD be written to refer to that 
new merge object. Then, any previous indirections for that ID MUST be deleted.

## Encoding

All objects are stored with bencoding, usually in a secret box:

    SB(k,x) = {
       "c": AES-256-GCM(key=k, plaintext=x, nonce=this.n, ad="")
       "n": randomly generated nonce
    }

Change records are stored as

    change_record = {
       '+': Value to be added (if any)
       '-': Value to be removed (if any)
       'k': Key to be modified
    }

Fragment reference records are stored as

    fragment_reference = {
       '@': GIT-SHA1(fragment)
       'x': list of 0-based indices of change records to exclude. Omitted if empty
    }

fragments are stored as

    fragment = SB(okey, {
       "h": list of previous heads
       "m": merge base
       "s": snapshot
       "x": list of change or change reference records
    })
    
* Simple changes have only one value in the `h` list and no `m` or `s` values. 
  Further, `x` contains no fragment reference records.
* Complex merges have all keys except `s`. `h` must contain at least one value.
* Simple merges only have `h`, and this list contains at least two items.
* Snapshots have only an `h` list containing a single element and an `s` value.

Indirections are stored as

    indir = SB(key, GIT-SHA1(fragment))
    
Keys 

Updates have the format



# Updates
* "change file" has been renamed to "fragment"