Introduction and Definitions
----------------------------

This protocol is used by two or more devices forming a mesh net.

The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT",
"SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" in this
document are to be interpreted as described in RFC 2119.

A _node_ is a single device implementing this protocol. Each node has
exactly one node address based on its RSA key pair.

A _node address_ consists of 32 bytes and is the SHA-256 hash of the
node's public key.

The _broadcast address_ is
`0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF`
(i.e. all bits set).

The _null address_ is
`0x0000000000000000000000000000000000000000000000000000000000000000`
(i.e. no bits set).

Nodes MUST NOT have a public key with the broadcast address or null
address as hash. Additionally, nodes MUST NOT connect to a node with
either address.


Messages
--------

All messages are signed using RSASSA-PKCS1-v1_5. All messages except
ConnectionInfo are encrypted using AES/CBC/PKCS5Padding, after which
the AES key is wrapped with the recipient's public RSA key.

     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    /                                                               /
    \                      Header (76 bytes)                        \
    /                                                               /
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    /                                                               /
    \               Encryption Data (variable length)               \
    /                                                               /
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    /                                                               /
    \                    Body (variable length)                     \
    /                                                               /
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+


### Header

Every message starts with one 76 byte header indicating the message
version, type and ID, followed by the length of the message. The
header is in network byte order, i.e. big endian.

     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |    Version    |     Type      |   Hop Limit   |   Hop Count   |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                            Length                             |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                                                               |
    |                   Origin Address (32 bytes)                   |
    |                                                               |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                                                               |
    |                   Target Address (32 bytes)                   |
    |                                                               |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |         Sequence Number       |           Reserved            |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

Version specifies the protocol version number. This is currently 0. A
message with unknown version number MUST be ignored. The connection
where such a packet came from MAY be closed.

Type is one of the message types specified below.

Hop Limit SHOULD be set to `MAX_HOP_COUNT` on message creation, and
MUST NOT be changed by a forwarding node.

Hop Count specifies the number of nodes a message may pass. When
creating a package, it is initialized to 0. Whenever a node forwards
a package, it MUST increment the hop limit by one. If the hop limit
BEFORE/AFTER? incrementing equals Hop Limit, the package MUST be
ignored.

Length is the message size in bytes, including the header.

Time is the unix timestamp of message creation, in seconds, as a
signed integer.

Origin Address is the address of the node that initially created the
message.

Target Address is the address of the node that should receive the
message.

Sequence number is set by the sender, and MUST increment by 1 for
each new message sent (after 2^16-1 comes 0 again). It SHOULD
be persistent during restarts. It is used by intermediate nodes
to avoid forwarding the same message multiple times.


### Encryption Data

     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |        Signature Length       |          Key Length           |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    /                                                               /
    \                   Signature (variable length)                 \
    /                                                               /
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    /                                                               /
    \                      Key (variable length)                    \
    /                                                               /
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

Encryption key is the symmetric key that was used to encrypt the message
body.

Signature is the cryptographic signature over the (unencrypted) message
header and message body.


ConnectionInfo (Type = 0)
---------

After successfully connecting to a node via Bluetooth, public keys
are exchanged. Each node MUST send this as the first message over
the connection. Hop Limit MUST be 1 for this message type (i.e. it
must never be forwarded). Origin Address and Target Address MUST be
set to all zeros, and MUST be ignored by the receiving node.

A receiving node SHOULD store the key in permanent storage if it
hasn't already stored it earlier.  However, a node MAY decide to
delete these stored keys in a least-recently-used order to adhere
to storage limitations. If a key has been deleted, messages to
that node can only be sent once a new ConnectionInfo message
for it has been received.


This key is to be used for message encryption when communicating
with the sending node.

     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                          Key Length                           |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    /                                                               /
    \                   Key (variable length)                       \
    /                                                               /
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

Key length is the size of the key in bytes.

Key is the public key of the sending node.

After this message has been received, communication with normal messages
may start.


### RequestAddContact (Type = 4)

Sent when a user wants to add another node as a contact. After this,
a ResultAddContact message should be returned.

     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                           Reserved                            |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+


### ResultAddContact (Type = 5)

Sent as response to a RequestAddContact message.

     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |A|                          Reserved                           |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

Accepted bit (A) is true if the user accepts the new contact, false
otherwise. Nodes should only add another node as a contact if both
users agreed.


### Text (Type = 6)

A simple chat message.

     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                             Time                              |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                          Text Length                          |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    /                                                               /
    \                   Text (variable length)                      \
    /                                                               /
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

Time is the unix timestamp of message sending.

Text the string to be transferred, encoded as UTF-8.

### UserName (Type = 7)

     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                          Name Length                          |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    /                                                               /
    \                   Name (variable length)                      \
    /                                                               /
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

Contains the sender's name, which should be used for display to users.
