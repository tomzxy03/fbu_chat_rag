debian-server@tomxzy:~$ cd coccoc-tokenizer/
debian-server@tomxzy:~/coccoc-tokenizer$ rm -rf build
debian-server@tomxzy:~/coccoc-tokenizer$ mkdir build && cd build
debian-server@tomxzy:~/coccoc-tokenizer/build$ cmake -DBUILD_JAVA=1 -DCMAKE_INSTALL_PREFIX=/home/debian-server/coccoc-install ..
CMake Warning (dev) at CMakeLists.txt:1 (PROJECT):
  cmake_minimum_required() should be called prior to this top-level project()
  call.  Please see the cmake-commands(7) manual for usage documentation of
  both commands.
This warning is for project developers.  Use -Wno-dev to suppress it.

-- The C compiler identification is GNU 14.2.0
-- The CXX compiler identification is GNU 14.2.0
-- Detecting C compiler ABI info
-- Detecting C compiler ABI info - done
-- Check for working C compiler: /usr/bin/cc - skipped
-- Detecting C compile features
-- Detecting C compile features - done
-- Detecting CXX compiler ABI info
-- Detecting CXX compiler ABI info - done
-- Check for working CXX compiler: /usr/bin/c++ - skipped
-- Detecting CXX compile features
-- Detecting CXX compile features - done
CMake Deprecation Warning at CMakeLists.txt:2 (CMAKE_MINIMUM_REQUIRED):
  Compatibility with CMake < 3.10 will be removed from a future version of
  CMake.

  Update the VERSION argument <min> value.  Or, use the <min>...<max> syntax
  to tell CMake that the project requires at least <min> but has been updated
  to work with policies introduced by <max> or earlier.


-- Configuring done (0.7s)
-- Generating done (0.0s)
-- Build files have been written to: /home/debian-server/coccoc-tokenizer/build
debian-server@tomxzy:~/coccoc-tokenizer/build$ make -j$(nproc)
[ 37%] Building CXX object CMakeFiles/tokenizer.dir/utils/tokenizer.cpp.o
[ 37%] Building CXX object CMakeFiles/vn_lang_tool.dir/utils/vn_lang_tool.cpp.o
[ 37%] Building CXX object CMakeFiles/dict_compiler.dir/utils/dict_compiler.cpp.o
[ 50%] Generating coccoc-tokenizer.jar
In file included from /home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/hash_trie_node.hpp:5,
                 from /home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/syllable_hash_trie_node.hpp:4,
                 from /home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/syllable_hash_trie.hpp:6,
                 from /home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie.hpp:4,
                 from /home/debian-server/coccoc-tokenizer/utils/dict_compiler.cpp:8:
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h: In instantiation of ‘void spp::sparsegroup<T, Alloc>::_set_val(value_type*, reference) [with T = std::pair<const int, float>; Alloc = spp::libc_allocator<std::pair<const int, float> >; value_type = std::pair<const int, float>; reference = std::pair<const int, float>&]’:
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1442:21:   required from ‘void spp::sparsegroup<T, Alloc>::_set(allocator_type&, size_type, size_type, Val&) [with Val = std::pair<const int, float>; T = std::pair<const int, float>; Alloc = spp::libc_allocator<std::pair<const int, float> >; allocator_type = spp::libc_allocator<std::pair<const int, float> >; size_type = unsigned char]’
 1442 |             _set_val(&_group[offset], val);
      |             ~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1455:13:   required from ‘spp::sparsegroup<T, Alloc>::value_type* spp::sparsegroup<T, Alloc>::set(allocator_type&, size_type, Val&) [with Val = std::pair<const int, float>; T = std::pair<const int, float>; Alloc = spp::libc_allocator<std::pair<const int, float> >; pointer = std::pair<const int, float>*; allocator_type = spp::libc_allocator<std::pair<const int, float> >; size_type = unsigned char]’
 1455 |         _set(alloc, i, offset, val);            // may change _group pointer
      |         ~~~~^~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:2243:28:   required from ‘spp::sparsetable<T, Alloc>::value_type& spp::sparsetable<T, Alloc>::set(size_type, Val&) [with Val = std::pair<const int, float>; T = std::pair<const int, float>; Alloc = spp::libc_allocator<std::pair<const int, float> >; reference = std::pair<const int, float>&; size_type = long unsigned int]’
 2243 |         pointer p(group.set(_alloc, pos_in_group(i), val));
      |                   ~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:3171:25:   required from ‘spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::value_type& spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::_insert_at(T&, size_type, bool) [with T = std::pair<const int, float>; Value = std::pair<const int, float>; Key = int; HashFcn = spp::spp_hash<int>; ExtractKey = spp::sparse_hash_map<int, float>::SelectKey; SetKey = spp::sparse_hash_map<int, float>::SetKey; EqualKey = std::equal_to<int>; Alloc = spp::libc_allocator<std::pair<const int, float> >; reference = std::pair<const int, float>&; size_type = long unsigned int]’
 3171 |         return table.set(pos, obj);
      |                ~~~~~~~~~^~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:3288:38:   required from ‘spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::value_type& spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::find_or_insert(KT&&) [with DefaultValue = spp::sparse_hash_map<int, float>::DefaultValue; KT = int&; Value = std::pair<const int, float>; Key = int; HashFcn = spp::spp_hash<int>; ExtractKey = spp::sparse_hash_map<int, float>::SelectKey; SetKey = spp::sparse_hash_map<int, float>::SetKey; EqualKey = std::equal_to<int>; Alloc = spp::libc_allocator<std::pair<const int, float> >; value_type = std::pair<const int, float>]’
 3288 |                     return _insert_at(def, erased ? erased_pos : bucknum, erased);
      |                            ~~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:3810:57:   required from ‘spp::sparse_hash_map<Key, T, HashFcn, EqualKey, Alloc>::mapped_type& spp::sparse_hash_map<Key, T, HashFcn, EqualKey, Alloc>::operator[](KT&&) [with KT = int&; Key = int; T = float; HashFcn = spp::spp_hash<int>; EqualKey = std::equal_to<int>; Alloc = spp::libc_allocator<std::pair<const int, float> >; mapped_type = float]’
 3810 |         return rep.template find_or_insert<DefaultValue>(std::forward<KT>(key)).second;
      |                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/utils/dict_compiler.cpp:178:24:   required from here
  178 |                         cur_map[second_index] = pair_score;
      |                                             ^
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1357:41: warning: casting ‘spp::sparsegroup<std::pair<const int, float>, spp::libc_allocator<std::pair<const int, float> > >::value_type’ {aka ‘std::pair<const int, float>’} to ‘spp::sparsegroup<std::pair<const int, float>, spp::libc_allocator<std::pair<const int, float> > >::mutable_reference’ {aka ‘std::pair<int, float>&’} does not use ‘constexpr std::pair<_T1, _T2>::pair(const std::pair<_U1, _U2>&) [with _U1 = const int; _U2 = float; typename std::enable_if<(std::_PCC<((! std::is_same<_T1, _U1>::value) || (! std::is_same<_T2, _U2>::value)), _T1, _T2>::_ConstructiblePair<_U1, _U2>() && std::_PCC<((! std::is_same<_T1, _U1>::value) || (! std::is_same<_T2, _U2>::value)), _T1, _T2>::_ImplicitlyConvertiblePair<_U1, _U2>()), bool>::type <anonymous> = true; _T1 = int; _T2 = float]’ [-Wcast-user-defined]
 1357 |         *(mutable_pointer)p = std::move((mutable_reference)val);
      |                                         ^~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h: In instantiation of ‘void spp::sparsegroup<T, Alloc>::_init_val(mutable_value_type*, reference) [with T = std::pair<const int, float>; Alloc = spp::libc_allocator<std::pair<const int, float> >; mutable_value_type = std::pair<int, float>; reference = std::pair<const int, float>&]’:
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1392:18:   required from ‘void spp::sparsegroup<T, Alloc>::_set_aux(allocator_type&, size_type, Val&, realloc_ok_type) [with Val = std::pair<const int, float>; T = std::pair<const int, float>; Alloc = spp::libc_allocator<std::pair<const int, float> >; allocator_type = spp::libc_allocator<std::pair<const int, float> >; size_type = unsigned char; realloc_ok_type = spp::integral_constant<bool, true>]’
 1392 |         _init_val((mutable_pointer)(_group + offset), val);
      |         ~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1437:21:   required from ‘void spp::sparsegroup<T, Alloc>::_set(allocator_type&, size_type, size_type, Val&) [with Val = std::pair<const int, float>; T = std::pair<const int, float>; Alloc = spp::libc_allocator<std::pair<const int, float> >; allocator_type = spp::libc_allocator<std::pair<const int, float> >; size_type = unsigned char]’
 1437 |             _set_aux(alloc, offset, val, check_alloc_type());
      |             ~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1455:13:   required from ‘spp::sparsegroup<T, Alloc>::value_type* spp::sparsegroup<T, Alloc>::set(allocator_type&, size_type, Val&) [with Val = std::pair<const int, float>; T = std::pair<const int, float>; Alloc = spp::libc_allocator<std::pair<const int, float> >; pointer = std::pair<const int, float>*; allocator_type = spp::libc_allocator<std::pair<const int, float> >; size_type = unsigned char]’
 1455 |         _set(alloc, i, offset, val);            // may change _group pointer
      |         ~~~~^~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:2243:28:   required from ‘spp::sparsetable<T, Alloc>::value_type& spp::sparsetable<T, Alloc>::set(size_type, Val&) [with Val = std::pair<const int, float>; T = std::pair<const int, float>; Alloc = spp::libc_allocator<std::pair<const int, float> >; reference = std::pair<const int, float>&; size_type = long unsigned int]’
 2243 |         pointer p(group.set(_alloc, pos_in_group(i), val));
      |                   ~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:3171:25:   required from ‘spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::value_type& spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::_insert_at(T&, size_type, bool) [with T = std::pair<const int, float>; Value = std::pair<const int, float>; Key = int; HashFcn = spp::spp_hash<int>; ExtractKey = spp::sparse_hash_map<int, float>::SelectKey; SetKey = spp::sparse_hash_map<int, float>::SetKey; EqualKey = std::equal_to<int>; Alloc = spp::libc_allocator<std::pair<const int, float> >; reference = std::pair<const int, float>&; size_type = long unsigned int]’
 3171 |         return table.set(pos, obj);
      |                ~~~~~~~~~^~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:3288:38:   required from ‘spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::value_type& spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::find_or_insert(KT&&) [with DefaultValue = spp::sparse_hash_map<int, float>::DefaultValue; KT = int&; Value = std::pair<const int, float>; Key = int; HashFcn = spp::spp_hash<int>; ExtractKey = spp::sparse_hash_map<int, float>::SelectKey; SetKey = spp::sparse_hash_map<int, float>::SetKey; EqualKey = std::equal_to<int>; Alloc = spp::libc_allocator<std::pair<const int, float> >; value_type = std::pair<const int, float>]’
 3288 |                     return _insert_at(def, erased ? erased_pos : bucknum, erased);
      |                            ~~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:3810:57:   required from ‘spp::sparse_hash_map<Key, T, HashFcn, EqualKey, Alloc>::mapped_type& spp::sparse_hash_map<Key, T, HashFcn, EqualKey, Alloc>::operator[](KT&&) [with KT = int&; Key = int; T = float; HashFcn = spp::spp_hash<int>; EqualKey = std::equal_to<int>; Alloc = spp::libc_allocator<std::pair<const int, float> >; mapped_type = float]’
 3810 |         return rep.template find_or_insert<DefaultValue>(std::forward<KT>(key)).second;
      |                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/utils/dict_compiler.cpp:178:24:   required from here
  178 |                         cur_map[second_index] = pair_score;
      |                                             ^
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1341:40: warning: casting ‘spp::sparsegroup<std::pair<const int, float>, spp::libc_allocator<std::pair<const int, float> > >::value_type’ {aka ‘std::pair<const int, float>’} to ‘spp::sparsegroup<std::pair<const int, float>, spp::libc_allocator<std::pair<const int, float> > >::mutable_reference’ {aka ‘std::pair<int, float>&’} does not use ‘constexpr std::pair<_T1, _T2>::pair(const std::pair<_U1, _U2>&) [with _U1 = const int; _U2 = float; typename std::enable_if<(std::_PCC<((! std::is_same<_T1, _U1>::value) || (! std::is_same<_T2, _U2>::value)), _T1, _T2>::_ConstructiblePair<_U1, _U2>() && std::_PCC<((! std::is_same<_T1, _U1>::value) || (! std::is_same<_T2, _U2>::value)), _T1, _T2>::_ImplicitlyConvertiblePair<_U1, _U2>()), bool>::type <anonymous> = true; _T1 = int; _T2 = float]’ [-Wcast-user-defined]
 1341 |         ::new (p) value_type(std::move((mutable_reference)val));
      |                                        ^~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h: In instantiation of ‘void spp::sparsegroup<T, Alloc>::_set_val(value_type*, reference) [with T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; value_type = std::pair<const unsigned int, int>; reference = std::pair<const unsigned int, int>&]’:
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1442:21:   required from ‘void spp::sparsegroup<T, Alloc>::_set(allocator_type&, size_type, size_type, Val&) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; allocator_type = spp::libc_allocator<std::pair<const unsigned int, int> >; size_type = unsigned char]’
 1442 |             _set_val(&_group[offset], val);
      |             ~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1455:13:   required from ‘spp::sparsegroup<T, Alloc>::value_type* spp::sparsegroup<T, Alloc>::set(allocator_type&, size_type, Val&) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; pointer = std::pair<const unsigned int, int>*; allocator_type = spp::libc_allocator<std::pair<const unsigned int, int> >; size_type = unsigned char]’
 1455 |         _set(alloc, i, offset, val);            // may change _group pointer
      |         ~~~~^~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:2243:28:   required from ‘spp::sparsetable<T, Alloc>::value_type& spp::sparsetable<T, Alloc>::set(size_type, Val&) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; reference = std::pair<const unsigned int, int>&; size_type = long unsigned int]’
 2243 |         pointer p(group.set(_alloc, pos_in_group(i), val));
      |                   ~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:3171:25:   required from ‘spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::value_type& spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::_insert_at(T&, size_type, bool) [with T = std::pair<const unsigned int, int>; Value = std::pair<const unsigned int, int>; Key = unsigned int; HashFcn = spp::spp_hash<unsigned int>; ExtractKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SelectKey; SetKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SetKey; EqualKey = std::equal_to<unsigned int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; reference = std::pair<const unsigned int, int>&; size_type = long unsigned int]’
 3171 |         return table.set(pos, obj);
      |                ~~~~~~~~~^~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:3288:38:   required from ‘spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::value_type& spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::find_or_insert(KT&&) [with DefaultValue = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::DefaultValue; KT = unsigned int&; Value = std::pair<const unsigned int, int>; Key = unsigned int; HashFcn = spp::spp_hash<unsigned int>; ExtractKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SelectKey; SetKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SetKey; EqualKey = std::equal_to<unsigned int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; value_type = std::pair<const unsigned int, int>]’
 3288 |                     return _insert_at(def, erased ? erased_pos : bucknum, erased);
      |                            ~~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:3810:57:   required from ‘spp::sparse_hash_map<Key, T, HashFcn, EqualKey, Alloc>::mapped_type& spp::sparse_hash_map<Key, T, HashFcn, EqualKey, Alloc>::operator[](KT&&) [with KT = unsigned int&; Key = unsigned int; T = int; HashFcn = spp::spp_hash<unsigned int>; EqualKey = std::equal_to<unsigned int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; mapped_type = int]’
 3810 |         return rep.template find_or_insert<DefaultValue>(std::forward<KT>(key)).second;
      |                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/hash_trie.hpp:26:19:   required from ‘void HashTrie<Node>::add_child(int, uint32_t) [with Node = SyllableHashTrieNode; uint32_t = unsigned int]’
   26 |                 pool[u].children[c] = pool.size();
      |                 ~~~~~~~~~~~~~~~~^
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/hash_trie.hpp:50:5:   required from ‘int HashTrie<Node>::add_new_term(const std::string&, int) [with Node = SyllableHashTrieNode; std::string = std::__cxx11::basic_string<char>]’
   50 |                                 add_child(cur_node, codepoint);
      |                                 ^~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/syllable_hash_trie.hpp:13:64:   required from here
   13 |                 int end_node = HashTrie< SyllableHashTrieNode >::add_new_term(s, frequency);
      |                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~^~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1357:41: warning: casting ‘spp::sparsegroup<std::pair<const unsigned int, int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::value_type’ {aka ‘std::pair<const unsigned int, int>’} to ‘spp::sparsegroup<std::pair<const unsigned int, int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::mutable_reference’ {aka ‘std::pair<unsigned int, int>&’} does not use ‘constexpr std::pair<_T1, _T2>::pair(const std::pair<_U1, _U2>&) [with _U1 = const unsigned int; _U2 = int; typename std::enable_if<(std::_PCC<((! std::is_same<_T1, _U1>::value) || (! std::is_same<_T2, _U2>::value)), _T1, _T2>::_ConstructiblePair<_U1, _U2>() && std::_PCC<((! std::is_same<_T1, _U1>::value) || (! std::is_same<_T2, _U2>::value)), _T1, _T2>::_ImplicitlyConvertiblePair<_U1, _U2>()), bool>::type <anonymous> = true; _T1 = unsigned int; _T2 = int]’ [-Wcast-user-defined]
 1357 |         *(mutable_pointer)p = std::move((mutable_reference)val);
      |                                         ^~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h: In instantiation of ‘void spp::sparsegroup<T, Alloc>::_init_val(mutable_value_type*, reference) [with T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; mutable_value_type = std::pair<unsigned int, int>; reference = std::pair<const unsigned int, int>&]’:
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1392:18:   required from ‘void spp::sparsegroup<T, Alloc>::_set_aux(allocator_type&, size_type, Val&, realloc_ok_type) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; allocator_type = spp::libc_allocator<std::pair<const unsigned int, int> >; size_type = unsigned char; realloc_ok_type = spp::integral_constant<bool, true>]’
 1392 |         _init_val((mutable_pointer)(_group + offset), val);
      |         ~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1437:21:   required from ‘void spp::sparsegroup<T, Alloc>::_set(allocator_type&, size_type, size_type, Val&) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; allocator_type = spp::libc_allocator<std::pair<const unsigned int, int> >; size_type = unsigned char]’
 1437 |             _set_aux(alloc, offset, val, check_alloc_type());
      |             ~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1455:13:   required from ‘spp::sparsegroup<T, Alloc>::value_type* spp::sparsegroup<T, Alloc>::set(allocator_type&, size_type, Val&) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; pointer = std::pair<const unsigned int, int>*; allocator_type = spp::libc_allocator<std::pair<const unsigned int, int> >; size_type = unsigned char]’
 1455 |         _set(alloc, i, offset, val);            // may change _group pointer
      |         ~~~~^~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:2243:28:   required from ‘spp::sparsetable<T, Alloc>::value_type& spp::sparsetable<T, Alloc>::set(size_type, Val&) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; reference = std::pair<const unsigned int, int>&; size_type = long unsigned int]’
 2243 |         pointer p(group.set(_alloc, pos_in_group(i), val));
      |                   ~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:3171:25:   required from ‘spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::value_type& spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::_insert_at(T&, size_type, bool) [with T = std::pair<const unsigned int, int>; Value = std::pair<const unsigned int, int>; Key = unsigned int; HashFcn = spp::spp_hash<unsigned int>; ExtractKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SelectKey; SetKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SetKey; EqualKey = std::equal_to<unsigned int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; reference = std::pair<const unsigned int, int>&; size_type = long unsigned int]’
 3171 |         return table.set(pos, obj);
      |                ~~~~~~~~~^~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:3288:38:   required from ‘spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::value_type& spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::find_or_insert(KT&&) [with DefaultValue = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::DefaultValue; KT = unsigned int&; Value = std::pair<const unsigned int, int>; Key = unsigned int; HashFcn = spp::spp_hash<unsigned int>; ExtractKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SelectKey; SetKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SetKey; EqualKey = std::equal_to<unsigned int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; value_type = std::pair<const unsigned int, int>]’
 3288 |                     return _insert_at(def, erased ? erased_pos : bucknum, erased);
      |                            ~~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:3810:57:   required from ‘spp::sparse_hash_map<Key, T, HashFcn, EqualKey, Alloc>::mapped_type& spp::sparse_hash_map<Key, T, HashFcn, EqualKey, Alloc>::operator[](KT&&) [with KT = unsigned int&; Key = unsigned int; T = int; HashFcn = spp::spp_hash<unsigned int>; EqualKey = std::equal_to<unsigned int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; mapped_type = int]’
 3810 |         return rep.template find_or_insert<DefaultValue>(std::forward<KT>(key)).second;
      |                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/hash_trie.hpp:26:19:   required from ‘void HashTrie<Node>::add_child(int, uint32_t) [with Node = SyllableHashTrieNode; uint32_t = unsigned int]’
   26 |                 pool[u].children[c] = pool.size();
      |                 ~~~~~~~~~~~~~~~~^
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/hash_trie.hpp:50:5:   required from ‘int HashTrie<Node>::add_new_term(const std::string&, int) [with Node = SyllableHashTrieNode; std::string = std::__cxx11::basic_string<char>]’
   50 |                                 add_child(cur_node, codepoint);
      |                                 ^~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/syllable_hash_trie.hpp:13:64:   required from here
   13 |                 int end_node = HashTrie< SyllableHashTrieNode >::add_new_term(s, frequency);
      |                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~^~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1341:40: warning: casting ‘spp::sparsegroup<std::pair<const unsigned int, int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::value_type’ {aka ‘std::pair<const unsigned int, int>’} to ‘spp::sparsegroup<std::pair<const unsigned int, int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::mutable_reference’ {aka ‘std::pair<unsigned int, int>&’} does not use ‘constexpr std::pair<_T1, _T2>::pair(const std::pair<_U1, _U2>&) [with _U1 = const unsigned int; _U2 = int; typename std::enable_if<(std::_PCC<((! std::is_same<_T1, _U1>::value) || (! std::is_same<_T2, _U2>::value)), _T1, _T2>::_ConstructiblePair<_U1, _U2>() && std::_PCC<((! std::is_same<_T1, _U1>::value) || (! std::is_same<_T2, _U2>::value)), _T1, _T2>::_ImplicitlyConvertiblePair<_U1, _U2>()), bool>::type <anonymous> = true; _T1 = unsigned int; _T2 = int]’ [-Wcast-user-defined]
 1341 |         ::new (p) value_type(std::move((mutable_reference)val));
      |                                        ^~~~~~~~~~~~~~~~~~~~~~
In file included from /home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/hash_trie_node.hpp:5,
                 from /home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/syllable_hash_trie_node.hpp:4,
                 from /home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/syllable_hash_trie.hpp:6,
                 from /home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie.hpp:4,
                 from /home/debian-server/coccoc-tokenizer/tokenizer/tokenizer.hpp:10,
                 from /home/debian-server/coccoc-tokenizer/utils/tokenizer.cpp:3:
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h: In instantiation of ‘void spp::sparsegroup<T, Alloc>::_set_val(value_type*, reference) [with T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; value_type = std::pair<const unsigned int, int>; reference = std::pair<const unsigned int, int>&]’:
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1442:21:   required from ‘void spp::sparsegroup<T, Alloc>::_set(allocator_type&, size_type, size_type, Val&) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; allocator_type = spp::libc_allocator<std::pair<const unsigned int, int> >; size_type = unsigned char]’
 1442 |             _set_val(&_group[offset], val);
      |             ~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1455:13:   required from ‘spp::sparsegroup<T, Alloc>::value_type* spp::sparsegroup<T, Alloc>::set(allocator_type&, size_type, Val&) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; pointer = std::pair<const unsigned int, int>*; allocator_type = spp::libc_allocator<std::pair<const unsigned int, int> >; size_type = unsigned char]’
 1455 |         _set(alloc, i, offset, val);            // may change _group pointer
      |         ~~~~^~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:2243:28:   required from ‘spp::sparsetable<T, Alloc>::value_type& spp::sparsetable<T, Alloc>::set(size_type, Val&) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; reference = std::pair<const unsigned int, int>&; size_type = long unsigned int]’
 2243 |         pointer p(group.set(_alloc, pos_in_group(i), val));
      |                   ~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:3171:25:   required from ‘spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::value_type& spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::_insert_at(T&, size_type, bool) [with T = std::pair<const unsigned int, int>; Value = std::pair<const unsigned int, int>; Key = unsigned int; HashFcn = spp::spp_hash<unsigned int>; ExtractKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SelectKey; SetKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SetKey; EqualKey = std::equal_to<unsigned int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; reference = std::pair<const unsigned int, int>&; size_type = long unsigned int]’
 3171 |         return table.set(pos, obj);
      |                ~~~~~~~~~^~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:3288:38:   required from ‘spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::value_type& spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::find_or_insert(KT&&) [with DefaultValue = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::DefaultValue; KT = unsigned int&; Value = std::pair<const unsigned int, int>; Key = unsigned int; HashFcn = spp::spp_hash<unsigned int>; ExtractKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SelectKey; SetKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SetKey; EqualKey = std::equal_to<unsigned int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; value_type = std::pair<const unsigned int, int>]’
 3288 |                     return _insert_at(def, erased ? erased_pos : bucknum, erased);
      |                            ~~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:3810:57:   required from ‘spp::sparse_hash_map<Key, T, HashFcn, EqualKey, Alloc>::mapped_type& spp::sparse_hash_map<Key, T, HashFcn, EqualKey, Alloc>::operator[](KT&&) [with KT = unsigned int&; Key = unsigned int; T = int; HashFcn = spp::spp_hash<unsigned int>; EqualKey = std::equal_to<unsigned int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; mapped_type = int]’
 3810 |         return rep.template find_or_insert<DefaultValue>(std::forward<KT>(key)).second;
      |                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/hash_trie.hpp:26:19:   required from ‘void HashTrie<Node>::add_child(int, uint32_t) [with Node = SyllableHashTrieNode; uint32_t = unsigned int]’
   26 |                 pool[u].children[c] = pool.size();
      |                 ~~~~~~~~~~~~~~~~^
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/hash_trie.hpp:50:5:   required from ‘int HashTrie<Node>::add_new_term(const std::string&, int) [with Node = SyllableHashTrieNode; std::string = std::__cxx11::basic_string<char>]’
   50 |                                 add_child(cur_node, codepoint);
      |                                 ^~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/syllable_hash_trie.hpp:13:64:   required from here
   13 |                 int end_node = HashTrie< SyllableHashTrieNode >::add_new_term(s, frequency);
      |                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~^~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1357:41: warning: casting ‘spp::sparsegroup<std::pair<const unsigned int, int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::value_type’ {aka ‘std::pair<const unsigned int, int>’} to ‘spp::sparsegroup<std::pair<const unsigned int, int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::mutable_reference’ {aka ‘std::pair<unsigned int, int>&’} does not use ‘constexpr std::pair<_T1, _T2>::pair(const std::pair<_U1, _U2>&) [with _U1 = const unsigned int; _U2 = int; typename std::enable_if<(std::_PCC<((! std::is_same<_T1, _U1>::value) || (! std::is_same<_T2, _U2>::value)), _T1, _T2>::_ConstructiblePair<_U1, _U2>() && std::_PCC<((! std::is_same<_T1, _U1>::value) || (! std::is_same<_T2, _U2>::value)), _T1, _T2>::_ImplicitlyConvertiblePair<_U1, _U2>()), bool>::type <anonymous> = true; _T1 = unsigned int; _T2 = int]’ [-Wcast-user-defined]
 1357 |         *(mutable_pointer)p = std::move((mutable_reference)val);
      |                                         ^~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h: In instantiation of ‘void spp::sparsegroup<T, Alloc>::_init_val(mutable_value_type*, reference) [with T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; mutable_value_type = std::pair<unsigned int, int>; reference = std::pair<const unsigned int, int>&]’:
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1392:18:   required from ‘void spp::sparsegroup<T, Alloc>::_set_aux(allocator_type&, size_type, Val&, realloc_ok_type) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; allocator_type = spp::libc_allocator<std::pair<const unsigned int, int> >; size_type = unsigned char; realloc_ok_type = spp::integral_constant<bool, true>]’
 1392 |         _init_val((mutable_pointer)(_group + offset), val);
      |         ~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1437:21:   required from ‘void spp::sparsegroup<T, Alloc>::_set(allocator_type&, size_type, size_type, Val&) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; allocator_type = spp::libc_allocator<std::pair<const unsigned int, int> >; size_type = unsigned char]’
 1437 |             _set_aux(alloc, offset, val, check_alloc_type());
      |             ~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1455:13:   required from ‘spp::sparsegroup<T, Alloc>::value_type* spp::sparsegroup<T, Alloc>::set(allocator_type&, size_type, Val&) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; pointer = std::pair<const unsigned int, int>*; allocator_type = spp::libc_allocator<std::pair<const unsigned int, int> >; size_type = unsigned char]’
 1455 |         _set(alloc, i, offset, val);            // may change _group pointer
      |         ~~~~^~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:2243:28:   required from ‘spp::sparsetable<T, Alloc>::value_type& spp::sparsetable<T, Alloc>::set(size_type, Val&) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; reference = std::pair<const unsigned int, int>&; size_type = long unsigned int]’
 2243 |         pointer p(group.set(_alloc, pos_in_group(i), val));
      |                   ~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:3171:25:   required from ‘spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::value_type& spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::_insert_at(T&, size_type, bool) [with T = std::pair<const unsigned int, int>; Value = std::pair<const unsigned int, int>; Key = unsigned int; HashFcn = spp::spp_hash<unsigned int>; ExtractKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SelectKey; SetKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SetKey; EqualKey = std::equal_to<unsigned int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; reference = std::pair<const unsigned int, int>&; size_type = long unsigned int]’
 3171 |         return table.set(pos, obj);
      |                ~~~~~~~~~^~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:3288:38:   required from ‘spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::value_type& spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::find_or_insert(KT&&) [with DefaultValue = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::DefaultValue; KT = unsigned int&; Value = std::pair<const unsigned int, int>; Key = unsigned int; HashFcn = spp::spp_hash<unsigned int>; ExtractKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SelectKey; SetKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SetKey; EqualKey = std::equal_to<unsigned int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; value_type = std::pair<const unsigned int, int>]’
 3288 |                     return _insert_at(def, erased ? erased_pos : bucknum, erased);
      |                            ~~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:3810:57:   required from ‘spp::sparse_hash_map<Key, T, HashFcn, EqualKey, Alloc>::mapped_type& spp::sparse_hash_map<Key, T, HashFcn, EqualKey, Alloc>::operator[](KT&&) [with KT = unsigned int&; Key = unsigned int; T = int; HashFcn = spp::spp_hash<unsigned int>; EqualKey = std::equal_to<unsigned int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; mapped_type = int]’
 3810 |         return rep.template find_or_insert<DefaultValue>(std::forward<KT>(key)).second;
      |                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/hash_trie.hpp:26:19:   required from ‘void HashTrie<Node>::add_child(int, uint32_t) [with Node = SyllableHashTrieNode; uint32_t = unsigned int]’
   26 |                 pool[u].children[c] = pool.size();
      |                 ~~~~~~~~~~~~~~~~^
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/hash_trie.hpp:50:5:   required from ‘int HashTrie<Node>::add_new_term(const std::string&, int) [with Node = SyllableHashTrieNode; std::string = std::__cxx11::basic_string<char>]’
   50 |                                 add_child(cur_node, codepoint);
      |                                 ^~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/syllable_hash_trie.hpp:13:64:   required from here
   13 |                 int end_node = HashTrie< SyllableHashTrieNode >::add_new_term(s, frequency);
      |                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~^~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/tokenizer/auxiliary/trie/../sparsepp/spp.h:1341:40: warning: casting ‘spp::sparsegroup<std::pair<const unsigned int, int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::value_type’ {aka ‘std::pair<const unsigned int, int>’} to ‘spp::sparsegroup<std::pair<const unsigned int, int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::mutable_reference’ {aka ‘std::pair<unsigned int, int>&’} does not use ‘constexpr std::pair<_T1, _T2>::pair(const std::pair<_U1, _U2>&) [with _U1 = const unsigned int; _U2 = int; typename std::enable_if<(std::_PCC<((! std::is_same<_T1, _U1>::value) || (! std::is_same<_T2, _U2>::value)), _T1, _T2>::_ConstructiblePair<_U1, _U2>() && std::_PCC<((! std::is_same<_T1, _U1>::value) || (! std::is_same<_T2, _U2>::value)), _T1, _T2>::_ImplicitlyConvertiblePair<_U1, _U2>()), bool>::type <anonymous> = true; _T1 = unsigned int; _T2 = int]’ [-Wcast-user-defined]
 1341 |         ::new (p) value_type(std::move((mutable_reference)val));
      |                                        ^~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/java/src/java/Unsafe.java:7: warning: Unsafe is internal proprietary API and may be removed in a future release
        public static final sun.misc.Unsafe UNSAFE;
                                    ^
/home/debian-server/coccoc-tokenizer/java/src/java/Unsafe.java:10: warning: Unsafe is internal proprietary API and may be removed in a future release
                sun.misc.Unsafe unsafe = null;
                        ^
/home/debian-server/coccoc-tokenizer/java/src/java/Unsafe.java:12: warning: Unsafe is internal proprietary API and may be removed in a future release
                        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                                              ^
/home/debian-server/coccoc-tokenizer/java/src/java/Unsafe.java:14: warning: Unsafe is internal proprietary API and may be removed in a future release
                        unsafe = (sun.misc.Unsafe) field.get(null);
                                          ^
/home/debian-server/coccoc-tokenizer/java/src/java/Unsafe.java:42: warning: Unsafe is internal proprietary API and may be removed in a future release
                return UNSAFE.getFloat(buffer, (long) (sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET + offset));
                                                               ^
/home/debian-server/coccoc-tokenizer/java/src/java/Unsafe.java:46: warning: Unsafe is internal proprietary API and may be removed in a future release
                return UNSAFE.getDouble(buffer, (long) (sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET + offset));
                                                                ^
/home/debian-server/coccoc-tokenizer/java/src/java/Unsafe.java:50: warning: Unsafe is internal proprietary API and may be removed in a future release
                return UNSAFE.getLong(buffer, (long) (sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET + offset));
                                                              ^
/home/debian-server/coccoc-tokenizer/java/src/java/Unsafe.java:54: warning: Unsafe is internal proprietary API and may be removed in a future release
                return UNSAFE.getInt(buffer, (long) (sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET + offset));
                                                             ^
/home/debian-server/coccoc-tokenizer/java/src/java/Unsafe.java:58: warning: Unsafe is internal proprietary API and may be removed in a future release
                return UNSAFE.getShort(buffer, (long) (sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET + offset));
                                                               ^
/home/debian-server/coccoc-tokenizer/java/src/java/Unsafe.java:62: warning: Unsafe is internal proprietary API and may be removed in a future release
                return UNSAFE.getByte(buffer, (long) (sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET + offset));
                                                              ^
/home/debian-server/coccoc-tokenizer/java/src/java/Unsafe.java:91: warning: Unsafe is internal proprietary API and may be removed in a future release
                UNSAFE.copyMemory(values, sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET, null, pointer, length);
                                                  ^
/home/debian-server/coccoc-tokenizer/java/src/java/Unsafe.java:95: warning: Unsafe is internal proprietary API and may be removed in a future release
                UNSAFE.copyMemory(values, sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET + off, null, pointer, length);
                                                  ^
/home/debian-server/coccoc-tokenizer/java/src/java/Unsafe.java:99: warning: Unsafe is internal proprietary API and may be removed in a future release
                UNSAFE.copyMemory(values, sun.misc.Unsafe.ARRAY_SHORT_BASE_OFFSET, null, pointer, length * Short.BYTES);
                                                  ^
/home/debian-server/coccoc-tokenizer/java/src/java/Unsafe.java:103: warning: Unsafe is internal proprietary API and may be removed in a future release
                UNSAFE.copyMemory(values, sun.misc.Unsafe.ARRAY_INT_BASE_OFFSET, null, pointer, length * Integer.BYTES);
                                                  ^
/home/debian-server/coccoc-tokenizer/java/src/java/Unsafe.java:107: warning: Unsafe is internal proprietary API and may be removed in a future release
                UNSAFE.copyMemory(values, sun.misc.Unsafe.ARRAY_LONG_BASE_OFFSET, null, pointer, length * Long.BYTES);
                                                  ^
15 warnings
[ 62%] Linking CXX executable vn_lang_tool
[ 62%] Built target vn_lang_tool
In file included from /home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/hash_trie_node.hpp:5,
                 from /home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/syllable_hash_trie_node.hpp:4,
                 from /home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/syllable_hash_trie.hpp:6,
                 from /home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie.hpp:4,
                 from /home/debian-server/coccoc-tokenizer/java/../tokenizer/tokenizer.hpp:10,
                 from /home/debian-server/coccoc-tokenizer/java/src/jni/Tokenizer.cpp:6:
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/../sparsepp/spp.h: In instantiation of ‘void spp::sparsegroup<T, Alloc>::_set_val(value_type*, reference) [with T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; value_type = std::pair<const unsigned int, int>; reference = std::pair<const unsigned int, int>&]’:
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/../sparsepp/spp.h:1442:21:   required from ‘void spp::sparsegroup<T, Alloc>::_set(allocator_type&, size_type, size_type, Val&) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; allocator_type = spp::libc_allocator<std::pair<const unsigned int, int> >; size_type = unsigned char]’
 1442 |             _set_val(&_group[offset], val);
      |             ~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/../sparsepp/spp.h:1455:13:   required from ‘spp::sparsegroup<T, Alloc>::value_type* spp::sparsegroup<T, Alloc>::set(allocator_type&, size_type, Val&) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; pointer = std::pair<const unsigned int, int>*; allocator_type = spp::libc_allocator<std::pair<const unsigned int, int> >; size_type = unsigned char]’
 1455 |         _set(alloc, i, offset, val);            // may change _group pointer
      |         ~~~~^~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/../sparsepp/spp.h:2243:28:   required from ‘spp::sparsetable<T, Alloc>::value_type& spp::sparsetable<T, Alloc>::set(size_type, Val&) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; reference = std::pair<const unsigned int, int>&; size_type = long unsigned int]’
 2243 |         pointer p(group.set(_alloc, pos_in_group(i), val));
      |                   ~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/../sparsepp/spp.h:3171:25:   required from ‘spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::value_type& spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::_insert_at(T&, size_type, bool) [with T = std::pair<const unsigned int, int>; Value = std::pair<const unsigned int, int>; Key = unsigned int; HashFcn = spp::spp_hash<unsigned int>; ExtractKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SelectKey; SetKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SetKey; EqualKey = std::equal_to<unsigned int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; reference = std::pair<const unsigned int, int>&; size_type = long unsigned int]’
 3171 |         return table.set(pos, obj);
      |                ~~~~~~~~~^~~~~~~~~~
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/../sparsepp/spp.h:3288:38:   required from ‘spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::value_type& spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::find_or_insert(KT&&) [with DefaultValue = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::DefaultValue; KT = unsigned int&; Value = std::pair<const unsigned int, int>; Key = unsigned int; HashFcn = spp::spp_hash<unsigned int>; ExtractKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SelectKey; SetKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SetKey; EqualKey = std::equal_to<unsigned int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; value_type = std::pair<const unsigned int, int>]’
 3288 |                     return _insert_at(def, erased ? erased_pos : bucknum, erased);
      |                            ~~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/../sparsepp/spp.h:3810:57:   required from ‘spp::sparse_hash_map<Key, T, HashFcn, EqualKey, Alloc>::mapped_type& spp::sparse_hash_map<Key, T, HashFcn, EqualKey, Alloc>::operator[](KT&&) [with KT = unsigned int&; Key = unsigned int; T = int; HashFcn = spp::spp_hash<unsigned int>; EqualKey = std::equal_to<unsigned int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; mapped_type = int]’
 3810 |         return rep.template find_or_insert<DefaultValue>(std::forward<KT>(key)).second;
      |                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/hash_trie.hpp:26:19:   required from ‘void HashTrie<Node>::add_child(int, uint32_t) [with Node = SyllableHashTrieNode; uint32_t = unsigned int]’
   26 |                 pool[u].children[c] = pool.size();
      |                 ~~~~~~~~~~~~~~~~^
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/hash_trie.hpp:50:5:   required from ‘int HashTrie<Node>::add_new_term(const std::string&, int) [with Node = SyllableHashTrieNode; std::string = std::__cxx11::basic_string<char>]’
   50 |                                 add_child(cur_node, codepoint);
      |                                 ^~~~~~~~~
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/syllable_hash_trie.hpp:13:64:   required from here
   13 |                 int end_node = HashTrie< SyllableHashTrieNode >::add_new_term(s, frequency);
      |                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~^~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/../sparsepp/spp.h:1357:41: warning: casting ‘spp::sparsegroup<std::pair<const unsigned int, int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::value_type’ {aka ‘std::pair<const unsigned int, int>’} to ‘spp::sparsegroup<std::pair<const unsigned int, int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::mutable_reference’ {aka ‘std::pair<unsigned int, int>&’} does not use ‘constexpr std::pair<_T1, _T2>::pair(const std::pair<_U1, _U2>&) [with _U1 = const unsigned int; _U2 = int; typename std::enable_if<(std::_PCC<((! std::is_same<_T1, _U1>::value) || (! std::is_same<_T2, _U2>::value)), _T1, _T2>::_ConstructiblePair<_U1, _U2>() && std::_PCC<((! std::is_same<_T1, _U1>::value) || (! std::is_same<_T2, _U2>::value)), _T1, _T2>::_ImplicitlyConvertiblePair<_U1, _U2>()), bool>::type <anonymous> = true; _T1 = unsigned int; _T2 = int]’ [-Wcast-user-defined]
 1357 |         *(mutable_pointer)p = std::move((mutable_reference)val);
      |                                         ^~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/../sparsepp/spp.h: In instantiation of ‘void spp::sparsegroup<T, Alloc>::_init_val(mutable_value_type*, reference) [with T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; mutable_value_type = std::pair<unsigned int, int>; reference = std::pair<const unsigned int, int>&]’:
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/../sparsepp/spp.h:1392:18:   required from ‘void spp::sparsegroup<T, Alloc>::_set_aux(allocator_type&, size_type, Val&, realloc_ok_type) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; allocator_type = spp::libc_allocator<std::pair<const unsigned int, int> >; size_type = unsigned char; realloc_ok_type = spp::integral_constant<bool, true>]’
 1392 |         _init_val((mutable_pointer)(_group + offset), val);
      |         ~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/../sparsepp/spp.h:1437:21:   required from ‘void spp::sparsegroup<T, Alloc>::_set(allocator_type&, size_type, size_type, Val&) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; allocator_type = spp::libc_allocator<std::pair<const unsigned int, int> >; size_type = unsigned char]’
 1437 |             _set_aux(alloc, offset, val, check_alloc_type());
      |             ~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/../sparsepp/spp.h:1455:13:   required from ‘spp::sparsegroup<T, Alloc>::value_type* spp::sparsegroup<T, Alloc>::set(allocator_type&, size_type, Val&) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; pointer = std::pair<const unsigned int, int>*; allocator_type = spp::libc_allocator<std::pair<const unsigned int, int> >; size_type = unsigned char]’
 1455 |         _set(alloc, i, offset, val);            // may change _group pointer
      |         ~~~~^~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/../sparsepp/spp.h:2243:28:   required from ‘spp::sparsetable<T, Alloc>::value_type& spp::sparsetable<T, Alloc>::set(size_type, Val&) [with Val = std::pair<const unsigned int, int>; T = std::pair<const unsigned int, int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; reference = std::pair<const unsigned int, int>&; size_type = long unsigned int]’
 2243 |         pointer p(group.set(_alloc, pos_in_group(i), val));
      |                   ~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/../sparsepp/spp.h:3171:25:   required from ‘spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::value_type& spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::_insert_at(T&, size_type, bool) [with T = std::pair<const unsigned int, int>; Value = std::pair<const unsigned int, int>; Key = unsigned int; HashFcn = spp::spp_hash<unsigned int>; ExtractKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SelectKey; SetKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SetKey; EqualKey = std::equal_to<unsigned int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; reference = std::pair<const unsigned int, int>&; size_type = long unsigned int]’
 3171 |         return table.set(pos, obj);
      |                ~~~~~~~~~^~~~~~~~~~
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/../sparsepp/spp.h:3288:38:   required from ‘spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::value_type& spp::sparse_hashtable<Value, Key, HashFcn, ExtractKey, SetKey, EqualKey, Alloc>::find_or_insert(KT&&) [with DefaultValue = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::DefaultValue; KT = unsigned int&; Value = std::pair<const unsigned int, int>; Key = unsigned int; HashFcn = spp::spp_hash<unsigned int>; ExtractKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SelectKey; SetKey = spp::sparse_hash_map<unsigned int, int, spp::spp_hash<unsigned int>, std::equal_to<unsigned int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::SetKey; EqualKey = std::equal_to<unsigned int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; value_type = std::pair<const unsigned int, int>]’
 3288 |                     return _insert_at(def, erased ? erased_pos : bucknum, erased);
      |                            ~~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/../sparsepp/spp.h:3810:57:   required from ‘spp::sparse_hash_map<Key, T, HashFcn, EqualKey, Alloc>::mapped_type& spp::sparse_hash_map<Key, T, HashFcn, EqualKey, Alloc>::operator[](KT&&) [with KT = unsigned int&; Key = unsigned int; T = int; HashFcn = spp::spp_hash<unsigned int>; EqualKey = std::equal_to<unsigned int>; Alloc = spp::libc_allocator<std::pair<const unsigned int, int> >; mapped_type = int]’
 3810 |         return rep.template find_or_insert<DefaultValue>(std::forward<KT>(key)).second;
      |                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~^~~~~~~~~~~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/hash_trie.hpp:26:19:   required from ‘void HashTrie<Node>::add_child(int, uint32_t) [with Node = SyllableHashTrieNode; uint32_t = unsigned int]’
   26 |                 pool[u].children[c] = pool.size();
      |                 ~~~~~~~~~~~~~~~~^
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/hash_trie.hpp:50:5:   required from ‘int HashTrie<Node>::add_new_term(const std::string&, int) [with Node = SyllableHashTrieNode; std::string = std::__cxx11::basic_string<char>]’
   50 |                                 add_child(cur_node, codepoint);
      |                                 ^~~~~~~~~
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/syllable_hash_trie.hpp:13:64:   required from here
   13 |                 int end_node = HashTrie< SyllableHashTrieNode >::add_new_term(s, frequency);
      |                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~^~~~~~~~~~~~~~
/home/debian-server/coccoc-tokenizer/java/../tokenizer/auxiliary/trie/../sparsepp/spp.h:1341:40: warning: casting ‘spp::sparsegroup<std::pair<const unsigned int, int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::value_type’ {aka ‘std::pair<const unsigned int, int>’} to ‘spp::sparsegroup<std::pair<const unsigned int, int>, spp::libc_allocator<std::pair<const unsigned int, int> > >::mutable_reference’ {aka ‘std::pair<unsigned int, int>&’} does not use ‘constexpr std::pair<_T1, _T2>::pair(const std::pair<_U1, _U2>&) [with _U1 = const unsigned int; _U2 = int; typename std::enable_if<(std::_PCC<((! std::is_same<_T1, _U1>::value) || (! std::is_same<_T2, _U2>::value)), _T1, _T2>::_ConstructiblePair<_U1, _U2>() && std::_PCC<((! std::is_same<_T1, _U1>::value) || (! std::is_same<_T2, _U2>::value)), _T1, _T2>::_ImplicitlyConvertiblePair<_U1, _U2>()), bool>::type <anonymous> = true; _T1 = unsigned int; _T2 = int]’ [-Wcast-user-defined]
 1341 |         ::new (p) value_type(std::move((mutable_reference)val));
      |                                        ^~~~~~~~~~~~~~~~~~~~~~
[ 75%] Linking CXX executable tokenizer
[ 75%] Built target tokenizer
[ 87%] Linking CXX executable dict_compiler
[ 87%] Built target dict_compiler
[100%] Generating multiterm_trie.dump, syllable_trie.dump, nontone_pair_freq_map.dump
[100%] Built target compile_java
[100%] Built target compile_dict
debian-server@tomxzy:~/coccoc-tokenizer/build$ make install
[ 25%] Built target dict_compiler
[ 50%] Built target tokenizer
[ 75%] Built target vn_lang_tool
[ 87%] Built target compile_dict
[100%] Built target compile_java
Install the project...
-- Install configuration: "RELEASE"
-- Installing: /home/debian-server/coccoc-install/bin/tokenizer
-- Installing: /home/debian-server/coccoc-install/bin/vn_lang_tool
-- Installing: /home/debian-server/coccoc-install/bin/dict_compiler
-- Installing: /home/debian-server/coccoc-install/include/tokenizer
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/sparsepp
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/sparsepp/spp_config.h
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/sparsepp/spp_smartptr.h
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/sparsepp/spp_memory.h
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/sparsepp/spp_traits.h
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/sparsepp/spp_dlalloc.h
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/sparsepp/spp_stdint.h
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/sparsepp/spp_timer.h
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/sparsepp/spp_utils.h
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/sparsepp/spp.h
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/trie
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/tsl
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/tsl/robin_map.h
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/tsl/robin_hash.h
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/tsl/robin_growth_policy.h
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/tsl/robin_set.h
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/utf8.h
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/utf8
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/utf8/unchecked.h
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/utf8/core.h
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/utf8/checked.h
-- Up-to-date: /home/debian-server/coccoc-install/include/tokenizer
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/tokenizer.hpp
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/helper.hpp
-- Up-to-date: /home/debian-server/coccoc-install/include/tokenizer/auxiliary
-- Up-to-date: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/sparsepp
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/trie.hpp
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/file_serializer.hpp
-- Up-to-date: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/trie
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/trie/multiterm_da_trie_node.hpp
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/trie/syllable_da_trie_node.hpp
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/trie/hash_trie_node.hpp
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/trie/da_trie.hpp
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/trie/multiterm_hash_trie.hpp
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/trie/da_trie_node.hpp
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/trie/syllable_hash_trie_node.hpp
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/trie/syllable_hash_trie.hpp
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/trie/multiterm_da_trie.hpp
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/trie/string_set_trie.hpp
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/trie/multiterm_hash_trie_node.hpp
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/trie/syllable_da_trie.hpp
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/trie/hash_trie.hpp
-- Up-to-date: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/tsl
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/buffered_reader.hpp
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/vn_lang_tool.hpp
-- Up-to-date: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/utf8
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/token.hpp
-- Up-to-date: /home/debian-server/coccoc-install/include/tokenizer
-- Up-to-date: /home/debian-server/coccoc-install/include/tokenizer/auxiliary
-- Up-to-date: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/sparsepp
-- Up-to-date: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/trie
-- Up-to-date: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/tsl
-- Up-to-date: /home/debian-server/coccoc-install/include/tokenizer/auxiliary/utf8
-- Installing: /home/debian-server/coccoc-install/include/tokenizer/config.h
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts_text
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts_text/vn_lang_tool
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts_text/vn_lang_tool/numeric
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts_text/vn_lang_tool/i_and_y.txt
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts_text/vn_lang_tool/d_and_gi.txt
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts_text/vn_lang_tool/alphabetic
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts_text/tokenizer
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts_text/tokenizer/chemical_comp
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts_text/tokenizer/acronyms
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts_text/tokenizer/Freq2NontoneUniFile
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts_text/tokenizer/keyword.freq
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts_text/tokenizer/special_token.strong
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts_text/tokenizer/nontone_pair_freq
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts_text/tokenizer/vndic_multiterm
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts_text/tokenizer/special_token.weak
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts/numeric
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts/i_and_y.txt
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts/d_and_gi.txt
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts/alphabetic
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts/multiterm_trie.dump
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts/syllable_trie.dump
-- Installing: /home/debian-server/coccoc-install/share/tokenizer/dicts/nontone_pair_freq_map.dump
-- Installing: /home/debian-server/coccoc-install/share/java/coccoc-tokenizer.jar
-- Installing: /home/debian-server/coccoc-install/lib/libcoccoc_tokenizer_jni.so